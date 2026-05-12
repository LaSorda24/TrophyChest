import {
  exchangeAccessCodeForAuthTokens,
  exchangeNpssoForAccessCode,
  exchangeRefreshTokenForAuthTokens,
  getProfileFromAccountId,
  getTitleTrophies,
  getUserPlayedGames,
  getUserTitles,
  getUserTrophiesEarnedForTitle
} from "psn-api";

const defaultPsnClient = {
  exchangeAccessCodeForAuthTokens,
  exchangeNpssoForAccessCode,
  exchangeRefreshTokenForAuthTokens,
  getProfileFromAccountId,
  getTitleTrophies,
  getUserPlayedGames,
  getUserTitles,
  getUserTrophiesEarnedForTitle
};

const npssoPattern = /^[A-Za-z0-9]{64}$/;
const protectedMessage = "Vuelve a vincular PlayStation con un NPSSO nuevo para renovar la sesion.";

// APUNTE: EL PROXY RECIBE EL NPSSO Y NORMALIZA LA RESPUESTA PARA QUE ANDROID NO DEPENDA DE PSN DIRECTAMENTE.
export function getPlayStationStatusPayload() {
  return {
    configured: true,
    authMode: "npsso",
    message: "PlayStation Network se vincula pegando un token NPSSO generado desde PlayStation.com."
  };
}

export async function getPlayStationAuthPayload({ npsso, psnClient = defaultPsnClient }) {
  const normalizedNpsso = validateNpsso(npsso);
  const accessCode = await withPsnErrors(
    () => psnClient.exchangeNpssoForAccessCode(normalizedNpsso),
    "No se pudo validar el NPSSO de PlayStation. Revisa que lo hayas copiado completo."
  );
  const tokens = await withPsnErrors(
    () => psnClient.exchangeAccessCodeForAuthTokens(accessCode),
    "PlayStation no pudo convertir el NPSSO en una sesion valida."
  );
  const authorization = { accessToken: tokens.accessToken };
  const accountId = accountIdFromIdToken(tokens.idToken);
  const profile = await resolveProfile(authorization, psnClient, accountId);

  return {
    linkedAccount: normalizeProfile(profile, accountId),
    auth: normalizeAuthTokens(tokens)
  };
}

export async function getPlayStationTitleHistoryPayload({
  refreshToken,
  psnClient = defaultPsnClient
}) {
  const tokens = await refreshAuthorization(refreshToken, psnClient);
  const response = await withPsnErrors(
    () => psnClient.getUserPlayedGames(
      { accessToken: tokens.accessToken },
      "me",
      {
        limit: 50,
        categories: "ps4_game,ps5_native_game"
      }
    ),
    "No se pudieron leer los juegos recientes de PlayStation. " + protectedMessage
  );

  return {
    auth: normalizeAuthTokens(tokens),
    totalItemCount: response.totalItemCount ?? response.titles?.length ?? 0,
    titles: normalizePlayedGames(response.titles || [])
  };
}

export async function getPlayStationTrophyTitlesPayload({
  refreshToken,
  psnClient = defaultPsnClient
}) {
  const tokens = await refreshAuthorization(refreshToken, psnClient);
  const response = await withPsnErrors(
    () => psnClient.getUserTitles(
      { accessToken: tokens.accessToken },
      "me",
      {
        limit: 800
      }
    ),
    "No se pudieron leer los titulos con trofeos de PlayStation. " + protectedMessage
  );

  return {
    auth: normalizeAuthTokens(tokens),
    totalItemCount: response.totalItemCount ?? response.trophyTitles?.length ?? 0,
    titles: normalizeTrophyTitles(response.trophyTitles || [])
  };
}

export async function getPlayStationAchievementsPayload({
  refreshToken,
  npCommunicationId,
  npServiceName,
  psnClient = defaultPsnClient
}) {
  const normalizedId = validateNpCommunicationId(npCommunicationId);
  const serviceName = normalizeNpServiceName(npServiceName);
  const tokens = await refreshAuthorization(refreshToken, psnClient);
  const authorization = { accessToken: tokens.accessToken };
  const options = {
    limit: 800,
    ...(serviceName ? { npServiceName: serviceName } : {})
  };

  const [titleResponse, userResponse] = await Promise.all([
    withPsnErrors(
      () => psnClient.getTitleTrophies(authorization, normalizedId, "all", options),
      "No se pudo leer la lista de trofeos de este titulo en PlayStation."
    ),
    withPsnErrors(
      () => psnClient.getUserTrophiesEarnedForTitle(authorization, "me", normalizedId, "all", options),
      "No se pudo leer el progreso de trofeos de este titulo en PlayStation. " + protectedMessage
    )
  ]);

  const trophies = normalizeAchievements(titleResponse.trophies || [], userResponse.trophies || []);
  const unlocked = trophies.filter((trophy) => trophy.unlocked).length;

  return {
    auth: normalizeAuthTokens(tokens),
    npCommunicationId: normalizedId,
    npServiceName: serviceName || "trophy2",
    trophySetVersion: titleResponse.trophySetVersion || userResponse.trophySetVersion || "",
    hasTrophyGroups: Boolean(titleResponse.hasTrophyGroups || userResponse.hasTrophyGroups),
    lastUpdatedDateTime: userResponse.lastUpdatedDateTime || null,
    summary: {
      total: trophies.length,
      unlocked,
      progress: trophies.length > 0 ? unlocked / trophies.length : 0
    },
    trophies
  };
}

export function validateNpsso(npsso) {
  const normalized = String(npsso || "").trim();
  if (!npssoPattern.test(normalized)) {
    const error = new Error("Invalid NPSSO.");
    error.statusCode = 400;
    error.publicMessage = "El NPSSO debe tener 64 caracteres alfanumericos.";
    throw error;
  }
  return normalized;
}

export function normalizePlayedGames(titles) {
  return titles
    .map((title) => {
      const titleId = String(title.titleId || "").trim();
      const name = String(title.localizedName || title.name || "").trim();
      if (!titleId || !name) return null;

      const conceptImages = title.concept?.media?.images || [];
      const conceptImage = preferredConceptImage(conceptImages);
      const primaryImage = firstNonBlank(
        title.localizedImageUrl,
        title.imageUrl,
        title.media?.screenshotUrl,
        conceptImage
      );
      const heroImage = firstNonBlank(
        title.media?.screenshotUrl,
        conceptImage,
        title.localizedImageUrl,
        title.imageUrl
      );
      const lastPlayedEpochSeconds = parseIsoDateSeconds(title.lastPlayedDateTime);

      return {
        titleId,
        name,
        localizedName: title.localizedName || null,
        imageUrl: primaryImage,
        heroImageUrl: heroImage,
        lastPlayedDateTime: title.lastPlayedDateTime || null,
        lastPlayedEpochSeconds,
        playDuration: title.playDuration || null,
        playtimeMinutes: parseIsoDurationMinutes(title.playDuration),
        playCount: Number.isFinite(title.playCount) ? title.playCount : 0,
        category: title.category || "unknown",
        service: title.service || "none",
        conceptId: title.concept?.id ?? null,
        conceptTitleIds: title.concept?.titleIds || [],
        supportedPlatforms: supportedPlatformsFor(title.category)
      };
    })
    .filter(Boolean)
    .sort((a, b) => (b.lastPlayedEpochSeconds || 0) - (a.lastPlayedEpochSeconds || 0));
}

export function normalizeTrophyTitles(trophyTitles) {
  return trophyTitles
    .map((title) => {
      const npCommunicationId = String(title.npCommunicationId || "").trim();
      const name = String(title.trophyTitleName || "").trim();
      if (!npCommunicationId || !name) return null;

      const total = trophyCountTotal(title.definedTrophies);
      const unlocked = trophyCountTotal(title.earnedTrophies);
      const progressPercent = Number.isFinite(title.progress) ? title.progress : 0;

      return {
        npCommunicationId,
        npServiceName: title.npServiceName || serviceNameForPlatform(title.trophyTitlePlatform),
        trophySetVersion: title.trophySetVersion || "",
        title: name,
        iconUrl: title.trophyTitleIconUrl || null,
        platform: title.trophyTitlePlatform || "",
        hasTrophyGroups: Boolean(title.hasTrophyGroups),
        hidden: Boolean(title.hiddenFlag),
        lastUpdatedDateTime: title.lastUpdatedDateTime || null,
        lastUpdatedEpochSeconds: parseIsoDateSeconds(title.lastUpdatedDateTime),
        definedTrophies: title.definedTrophies || {},
        earnedTrophies: title.earnedTrophies || {},
        summary: {
          total,
          unlocked,
          progress: total > 0 ? unlocked / total : progressPercent / 100
        }
      };
    })
    .filter(Boolean)
    .sort((a, b) => (b.lastUpdatedEpochSeconds || 0) - (a.lastUpdatedEpochSeconds || 0));
}

export function normalizeAchievements(titleTrophies, userTrophies) {
  const userById = new Map(userTrophies.map((trophy) => [String(trophy.trophyId), trophy]));
  const titleById = new Map(titleTrophies.map((trophy) => [String(trophy.trophyId), trophy]));
  const orderedIds = [
    ...titleTrophies.map((trophy) => String(trophy.trophyId)),
    ...userTrophies.map((trophy) => String(trophy.trophyId))
  ].filter((id, index, ids) => ids.indexOf(id) === index);

  return orderedIds.map((id) => {
    const metadata = titleById.get(id) || {};
    const user = userById.get(id) || {};
    const earnedRate = parseNullableNumber(user.trophyEarnedRate ?? metadata.trophyEarnedRate);

    return {
      trophyId: Number(id),
      apiName: id,
      title: firstNonBlank(metadata.trophyName, user.trophyName, `Trofeo ${id}`),
      description: firstNonBlank(metadata.trophyDetail, user.trophyDetail, ""),
      hidden: Boolean(metadata.trophyHidden || user.trophyHidden),
      unlocked: Boolean(user.earned),
      unlockTime: parseIsoDateSeconds(user.earnedDateTime),
      unlockDateTime: user.earnedDateTime || null,
      globalPercentage: earnedRate,
      rarity: user.trophyRare ?? null,
      trophyType: metadata.trophyType || user.trophyType || "",
      groupId: metadata.trophyGroupId || user.trophyGroupId || "default",
      iconUrl: metadata.trophyIconUrl || user.trophyRewardImageUrl || null,
      lockedIconUrl: metadata.trophyIconUrl || null
    };
  });
}

export function parseIsoDurationMinutes(duration) {
  if (!duration || typeof duration !== "string") return 0;
  const match = duration.match(/^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/);
  if (!match) return 0;
  const days = Number(match[1] || 0);
  const hours = Number(match[2] || 0);
  const minutes = Number(match[3] || 0);
  const seconds = Number(match[4] || 0);
  return Math.floor(days * 24 * 60 + hours * 60 + minutes + seconds / 60);
}

function validateNpCommunicationId(npCommunicationId) {
  const normalized = String(npCommunicationId || "").trim();
  if (!/^[A-Za-z0-9_-]+$/.test(normalized)) {
    const error = new Error("Invalid PlayStation title.");
    error.statusCode = 400;
    error.publicMessage = "Titulo PlayStation no valido.";
    throw error;
  }
  return normalized;
}

function normalizeNpServiceName(npServiceName) {
  const normalized = String(npServiceName || "").trim();
  if (!normalized) return null;
  if (normalized !== "trophy" && normalized !== "trophy2") {
    const error = new Error("Invalid PlayStation trophy service.");
    error.statusCode = 400;
    error.publicMessage = "Servicio de trofeos PlayStation no valido.";
    throw error;
  }
  return normalized;
}

async function refreshAuthorization(refreshToken, psnClient) {
  const normalized = String(refreshToken || "").trim();
  if (!normalized) {
    const error = new Error("Missing PlayStation refresh token.");
    error.statusCode = 401;
    error.publicMessage = "Vincula PlayStation antes de sincronizar juegos y trofeos.";
    throw error;
  }

  return withPsnErrors(
    () => psnClient.exchangeRefreshTokenForAuthTokens(normalized),
    "La sesion de PlayStation ha caducado. " + protectedMessage
  );
}

export function accountIdFromIdToken(idToken) {
  const parts = String(idToken || "").split(".");
  if (parts.length < 2) return null;

  try {
    const payload = JSON.parse(decodeBase64Url(parts[1]));
    return firstNonBlank(
      payload.accountId,
      payload.account_id,
      payload.sub,
      payload.user_id
    );
  } catch {
    return null;
  }
}

async function resolveProfile(authorization, psnClient, accountId) {
  const requestedAccountId = accountId || "me";
  return withPsnErrors(
    () => psnClient.getProfileFromAccountId(authorization, requestedAccountId),
    null
  ).catch(() => null);
}

export function normalizeProfile(profileResponse, fallbackAccountId = null) {
  const profile = profileResponse?.profile || profileResponse || {};
  const avatars = profile.avatars || profile.avatarUrls || [];
  const avatarUrl = avatars.find((avatar) => avatar.size === "l")?.url ||
    avatars.find((avatar) => avatar.size === "l")?.avatarUrl ||
    avatars[0]?.url ||
    avatars[0]?.avatarUrl ||
    null;

  return {
    accountId: profile.accountId || fallbackAccountId || "me",
    onlineId: profile.onlineId || profile.userName || null,
    displayName: profile.onlineId || profile.userName || "Cuenta PlayStation vinculada",
    avatarUrl,
    isPlus: Boolean(profile.isPlus || profile.plus === 1)
  };
}

function normalizeAuthTokens(tokens) {
  return {
    refreshToken: tokens.refreshToken,
    refreshTokenExpiresIn: tokens.refreshTokenExpiresIn ?? null,
    accessTokenExpiresIn: tokens.expiresIn ?? null,
    tokenType: tokens.tokenType || "Bearer"
  };
}

async function withPsnErrors(action, publicMessage) {
  try {
    return await action();
  } catch (error) {
    if (!publicMessage) throw error;
    const wrapped = new Error(error?.message || publicMessage);
    wrapped.statusCode = error?.statusCode || error?.status || 502;
    wrapped.publicMessage = publicMessage;
    throw wrapped;
  }
}

function preferredConceptImage(images) {
  const preferredTypes = ["FOUR_BY_THREE_BANNER", "BACKGROUND", "MASTER", "SCREENSHOT"];
  for (const type of preferredTypes) {
    const match = images.find((image) => image.type === type && image.url);
    if (match) return match.url;
  }
  return images.find((image) => image.url)?.url || null;
}

function supportedPlatformsFor(category) {
  if (category === "ps5_native_game") return ["PlayStation 5"];
  if (category === "ps4_game") return ["PlayStation 4"];
  if (category === "pspc_game") return ["PlayStation PC"];
  return ["PlayStation"];
}

function serviceNameForPlatform(platform) {
  const normalized = String(platform || "").toLowerCase();
  return normalized.includes("ps5") ? "trophy2" : "trophy";
}

function trophyCountTotal(counts = {}) {
  return ["bronze", "silver", "gold", "platinum"]
    .map((key) => Number(counts[key] || 0))
    .reduce((sum, count) => sum + count, 0);
}

function parseIsoDateSeconds(value) {
  if (!value) return null;
  const millis = Date.parse(value);
  return Number.isFinite(millis) ? Math.floor(millis / 1000) : null;
}

function parseNullableNumber(value) {
  if (value === null || value === undefined || value === "") return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function firstNonBlank(...values) {
  return values.find((value) => typeof value === "string" && value.trim().length > 0)?.trim() || null;
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");

  if (typeof atob === "function") {
    return atob(padded);
  }

  return Buffer.from(padded, "base64").toString("utf8");
}
