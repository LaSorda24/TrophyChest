const IGDB_API_BASE_URL = "https://api.igdb.com/v4";
const TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";

const tokenCache = {
  accessToken: null,
  expiresAt: 0
};

// APUNTE: IGDB USA CREDENCIALES DE TWITCH; POR ESO ESTA LOGICA VIVE EN EL PROXY Y NO EN LA APP.
export function createTokenProvider(fetchFn, nowFn) {
  let cachedToken = null;
  let expiresAt = 0;

  return async function getCachedAccessToken(clientId, clientSecret) {
    const now = nowFn();
    if (cachedToken && now < expiresAt) {
      return cachedToken;
    }

    const token = await fetchAccessToken(clientId, clientSecret, fetchFn);
    cachedToken = token.accessToken;
    expiresAt = now + Math.max(0, token.expiresIn - 60) * 1000;
    return cachedToken;
  };
}

export async function getAccessToken(clientId, clientSecret, fetchFn, nowMs) {
  if (tokenCache.accessToken && nowMs < tokenCache.expiresAt) {
    return tokenCache.accessToken;
  }

  const token = await fetchAccessToken(clientId, clientSecret, fetchFn);
  tokenCache.accessToken = token.accessToken;
  tokenCache.expiresAt = nowMs + Math.max(0, token.expiresIn - 60) * 1000;

  return tokenCache.accessToken;
}

export async function postIgdb(fetchFn, endpoint, body, clientId, token) {
  const response = await fetchFn(`${IGDB_API_BASE_URL}/${endpoint}`, {
    method: "POST",
    headers: {
      "Accept": "application/json",
      "Client-ID": clientId,
      "Authorization": `Bearer ${token}`
    },
    body
  });

  if (!response.ok) {
    throw publicError(response.status, "IGDB no ha respondido correctamente.");
  }

  return response;
}

export function coverImageUrl(imageId, size = "t_cover_big") {
  return imageId
    ? `https://images.igdb.com/igdb/image/upload/${size}/${imageId}.jpg`
    : null;
}

export function normalizeIgdbImageUrl(rawUrl, size = "t_cover_big") {
  if (typeof rawUrl !== "string" || rawUrl.trim() === "") {
    return null;
  }

  const trimmed = rawUrl.trim();
  const withProtocol = trimmed.startsWith("//") ? `https:${trimmed}` : trimmed;

  return withProtocol.replace(/\/t_[^/]+\//, `/${size}/`);
}

export function imageUrlFromAsset(asset, size = "t_cover_big") {
  if (!asset) return null;
  return normalizeIgdbImageUrl(asset.url, size) || coverImageUrl(asset.image_id, size);
}

export function publicError(statusCode, publicMessage) {
  const error = new Error(publicMessage);
  error.statusCode = statusCode;
  error.publicMessage = publicMessage;
  return error;
}

async function fetchAccessToken(clientId, clientSecret, fetchFn) {
  if (!clientId || !clientSecret) {
    throw publicError(500, "Faltan TWITCH_CLIENT_ID o TWITCH_CLIENT_SECRET en el backend.");
  }

  const tokenUrl = new URL(TWITCH_TOKEN_URL);
  tokenUrl.searchParams.set("client_id", clientId);
  tokenUrl.searchParams.set("client_secret", clientSecret);
  tokenUrl.searchParams.set("grant_type", "client_credentials");

  const response = await fetchFn(tokenUrl, { method: "POST" });
  if (!response.ok) {
    throw publicError(response.status, "Twitch no ha podido generar un token para IGDB.");
  }

  const body = await response.json();
  return {
    accessToken: body.access_token,
    expiresIn: body.expires_in || 0
  };
}
