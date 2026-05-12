import {
  coverImageUrl,
  getAccessToken,
  postIgdb,
  publicError
} from "./igdbClient.js";
export { coverImageUrl } from "./igdbClient.js";
export { createTokenProvider } from "./igdbClient.js";

const DEFAULT_DAYS = 90;
const MAX_DAYS = 180;
const MAX_LIMIT = 200;
const PLATFORM_SLUGS = ["win", "ps4--1", "ps5", "xboxone", "series-x-s"];

const platformCache = {
  ids: null,
  expiresAt: 0
};

// APUNTE: EL CALENDARIO BUSCA LANZAMIENTOS EN IGDB Y LOS AGRUPA POR JUEGO, FECHA Y PLATAFORMA.
export async function getReleaseCalendarPayload({ from, days, limit, clientId, clientSecret, fetchFn, now }) {
  const safeFrom = parseIsoDate(from) || dateToIso(now);
  const safeDays = clampDays(days);
  const safeLimit = clampLimit(limit);
  const to = addDays(safeFrom, safeDays);
  const token = await getAccessToken(clientId, clientSecret, fetchFn, now.getTime());
  const platformIds = await resolvePlatformIds(fetchFn, clientId, token, now.getTime());

  const query = buildReleaseDatesQuery({
    fromEpoch: isoToUnixSeconds(safeFrom),
    toEpoch: isoToUnixSeconds(to),
    platformIds
  });

  const response = await postIgdb(fetchFn, "release_dates", query, clientId, token);
  const rawReleases = await response.json();

  return {
    generatedAt: now.toISOString(),
    from: safeFrom,
    to,
    releases: applyReleaseLimit(normalizeReleaseCalendar(rawReleases), safeLimit)
  };
}

async function resolvePlatformIds(fetchFn, clientId, token, nowMs) {
  if (platformCache.ids && nowMs < platformCache.expiresAt) {
    return platformCache.ids;
  }

  const query = [
    "fields id,name,slug;",
    `where slug = (${PLATFORM_SLUGS.map((slug) => `"${slug}"`).join(",")});`,
    "limit 10;"
  ].join(" ");
  const response = await postIgdb(fetchFn, "platforms", query, clientId, token);
  const platforms = await response.json();
  const ids = platforms
    .filter((platform) => PLATFORM_SLUGS.includes(platform.slug))
    .map((platform) => platform.id);

  if (ids.length === 0) {
    throw publicError(502, "IGDB no ha devuelto las plataformas configuradas.");
  }

  platformCache.ids = ids;
  platformCache.expiresAt = nowMs + 24 * 60 * 60 * 1000;
  return ids;
}

export function buildReleaseDatesQuery({ fromEpoch, toEpoch, platformIds }) {
  return [
    "fields date,human,date_format,release_region,platform.name,platform.slug,game.id,game.name,game.slug,game.category,game.cover.image_id,game.summary;",
    `where date >= ${fromEpoch} & date < ${toEpoch} & date_format = 0 & release_region = (1,8) & platform = (${platformIds.join(",")}) & game.category = null;`,
    "sort date asc;",
    "limit 200;"
  ].join(" ");
}

export function normalizeReleaseCalendar(rawReleases) {
  const grouped = new Map();

  rawReleases.forEach((release) => {
    const game = release.game;
    const releaseDate = unixSecondsToIso(release.date);
    const platform = release.platform;

    if (!game || !game.id || !game.name || !releaseDate || !platform) {
      return;
    }

    const key = `${game.id}:${releaseDate}`;
    const family = platformFamilyForSlug(platform.slug);
    const existing = grouped.get(key) || {
      igdbGameId: game.id,
      name: game.name,
      releaseDate,
      humanDate: release.human || releaseDate,
      coverUrl: coverImageUrl(game.cover && game.cover.image_id),
      summary: game.summary || null,
      platformFamilies: new Set(),
      platformNames: new Set()
    };

    if (family) {
      existing.platformFamilies.add(family);
    }
    if (platform.name) {
      existing.platformNames.add(platform.name);
    }

    grouped.set(key, existing);
  });

  return Array.from(grouped.values())
    .map((release) => ({
      ...release,
      platformFamilies: Array.from(release.platformFamilies),
      platformNames: Array.from(release.platformNames).sort()
    }))
    .sort((a, b) => a.releaseDate.localeCompare(b.releaseDate) || a.name.localeCompare(b.name));
}

export function applyReleaseLimit(releases, limit) {
  if (!Number.isFinite(limit)) {
    return releases;
  }
  return releases.slice(0, limit);
}

export function platformFamilyForSlug(slug) {
  if (slug === "win") return "PC";
  if (slug === "ps4--1" || slug === "ps5") return "PLAYSTATION";
  if (slug === "xboxone" || slug === "series-x-s") return "XBOX";
  return null;
}

function parseIsoDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return null;
  }
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return Number.isNaN(parsed.getTime()) ? null : value;
}

function clampDays(value) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) return DEFAULT_DAYS;
  return Math.min(Math.max(parsed, 1), MAX_DAYS);
}

function clampLimit(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) return null;
  return Math.min(Math.max(parsed, 1), MAX_LIMIT);
}

function addDays(isoDate, days) {
  const date = new Date(`${isoDate}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return dateToIso(date);
}

function isoToUnixSeconds(isoDate) {
  return Math.floor(new Date(`${isoDate}T00:00:00.000Z`).getTime() / 1000);
}

function unixSecondsToIso(value) {
  if (!Number.isFinite(value)) return null;
  return dateToIso(new Date(value * 1000));
}

function dateToIso(date) {
  return date.toISOString().slice(0, 10);
}
