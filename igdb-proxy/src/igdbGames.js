import {
  getAccessToken,
  imageUrlFromAsset,
  postIgdb,
  publicError
} from "./igdbClient.js";

const DEFAULT_SEARCH_LIMIT = 25;
const DEFAULT_EXPLORE_LIMIT = 20;
const DEFAULT_CATEGORY_LIMIT = 30;
const STEAM_FALLBACK_ID_OFFSET = 800000000000;
const EXPLORE_PLATFORM_SLUGS = ["win", "ps4--1", "ps5", "xboxone", "series-x-s"];
const INDIE_GENRE_NAME = "Indie";

const explorePlatformCache = {
  ids: null,
  expiresAt: 0
};

const genreCache = {
  idsByName: new Map(),
  expiresAt: 0
};

const themeCache = {
  idsByName: new Map(),
  expiresAt: 0
};

// APUNTE: BUSQUEDA COMBINA IGDB Y UN FALLBACK DE STEAM PARA MEJORAR RESULTADOS CONOCIDOS.
export async function getSearchGamesPayload({ query, clientId, clientSecret, fetchFn, now }) {
  const normalizedQuery = normalizeSearchQuery(query);
  if (!normalizedQuery) {
    return {
      query: "",
      games: []
    };
  }

  const token = await getAccessToken(clientId, clientSecret, fetchFn, now.getTime());
  const platformIds = await resolveExplorePlatformIds(fetchFn, clientId, token, now.getTime());
  const [exactResponse, slugResponse, searchResponse] = await Promise.all([
    postIgdb(
      fetchFn,
      "games",
      buildExactSearchGamesQuery(normalizedQuery, platformIds),
      clientId,
      token
    ),
    postIgdb(
      fetchFn,
      "games",
      buildSlugSearchGamesQuery(normalizedQuery, platformIds),
      clientId,
      token
    ),
    postIgdb(
      fetchFn,
      "games",
      buildSearchGamesQuery(normalizedQuery, platformIds),
      clientId,
      token
    )
  ]);
  const exactGames = await exactResponse.json();
  const slugGames = await slugResponse.json();
  const searchedGames = await searchResponse.json();

  const igdbGames = normalizeSearchGames([...exactGames, ...slugGames, ...searchedGames]);
  const steamFallbackGames = await getSteamSearchFallbackGames(normalizedQuery, fetchFn);

  return {
    query: normalizedQuery,
    games: mergeSearchGames([...steamFallbackGames, ...igdbGames])
  };
}

export async function getGameDetailsPayload({ gameId, clientId, clientSecret, fetchFn, now }) {
  const numericGameId = Number.parseInt(gameId, 10);
  if (!Number.isFinite(numericGameId) || numericGameId <= 0) {
    throw publicError(400, "El identificador del juego de IGDB no es valido.");
  }

  if (isSteamFallbackId(numericGameId)) {
    return getSteamFallbackGameDetailsPayload({
      syntheticId: numericGameId,
      fetchFn
    });
  }

  const token = await getAccessToken(clientId, clientSecret, fetchFn, now.getTime());
  const response = await postIgdb(
    fetchFn,
    "games",
    buildGameDetailsQuery(numericGameId),
    clientId,
    token
  );
  const rawGames = await response.json();
  const game = normalizeGameDetails(rawGames[0]);

  if (!game) {
    throw publicError(404, "IGDB no ha encontrado ese juego.");
  }

  return game;
}

// APUNTE: EXPLORAR PIDE SECCIONES DE JUEGOS Y PUEDE PERSONALIZAR POR GENEROS DEL USUARIO.
export async function getExploreGamesPayload({ clientId, clientSecret, fetchFn, now, genres }) {
  const token = await getAccessToken(clientId, clientSecret, fetchFn, now.getTime());
  const platformIds = await resolveExplorePlatformIds(fetchFn, clientId, token, now.getTime());
  const recommendationGenreNames = normalizeExploreGenreNames(genres);
  const recommendationGenreIds = recommendationGenreNames.length > 0
    ? await resolveGenreIds(fetchFn, clientId, token, recommendationGenreNames, now.getTime())
    : [];
  const indieGenreIds = await resolveGenreIds(fetchFn, clientId, token, [INDIE_GENRE_NAME], now.getTime());
  const queries = buildExploreGamesQueries({
    platformIds,
    now,
    recommendationGenreIds,
    indieGenreIds
  });

  const responses = await Promise.all(
    Object.entries(queries).map(async ([key, query]) => {
      const response = await postIgdb(fetchFn, "games", query, clientId, token);
      return [key, await response.json()];
    })
  );
  const sections = Object.fromEntries(responses);

  if (normalizeExploreReleasedGames(sections.topSellers, now).length < 10) {
    const response = await postIgdb(
      fetchFn,
      "games",
      buildTopSellersFallbackQuery({ platformIds, now }),
      clientId,
      token
    );
    sections.topSellers = [
      ...sections.topSellers,
      ...(await response.json())
    ];
  }

  if (recommendationGenreIds.length > 0 && normalizeExploreGames(sections.recommended).length === 0) {
    const response = await postIgdb(
      fetchFn,
      "games",
      buildRecommendedFallbackQuery({ platformIds }),
      clientId,
      token
    );
    sections.recommended = await response.json();
  }

  return buildExploreGamesPayload(
    sections,
    now,
    recommendationGenreIds.length > 0 ? recommendationGenreNames : []
  );
}

export async function getCategoryGamesPayload({ genre, clientId, clientSecret, fetchFn, now }) {
  const genreName = normalizeCategoryGenreName(genre);
  if (!genreName) {
    throw publicError(400, "Indica un genero valido para cargar la categoria.");
  }

  const token = await getAccessToken(clientId, clientSecret, fetchFn, now.getTime());
  const platformIds = await resolveExplorePlatformIds(fetchFn, clientId, token, now.getTime());
  const genreIds = await resolveGenreIds(fetchFn, clientId, token, [genreName], now.getTime());
  const themeIds = await resolveThemeIds(fetchFn, clientId, token, [genreName], now.getTime());

  if (genreIds.length === 0 && themeIds.length === 0) {
    throw publicError(404, "IGDB no ha encontrado ese genero o tema.");
  }

  const response = await postIgdb(
    fetchFn,
    "games",
    buildCategoryGamesQuery({ platformIds, genreIds, themeIds }),
    clientId,
    token
  );
  const rawGames = await response.json();

  return {
    genre: genreName,
    generatedAt: now.toISOString(),
    games: normalizeExploreGames(rawGames).slice(0, DEFAULT_CATEGORY_LIMIT)
  };
}

export function buildSearchGamesQuery(query, platformIds = []) {
  const escapedQuery = escapeApicalypseString(query);
  const platformFilter = platformIds.length > 0 ? ` & platforms = (${platformIds.join(",")})` : "";

  return [
    "fields id,name,cover.url,cover.image_id,platforms.name,platforms.slug,total_rating_count,hypes,first_release_date,version_parent,parent_game,category;",
    `search "${escapedQuery}";`,
    `where version_parent = null & parent_game = null & (category = 0 | category = null)${platformFilter};`,
    `limit ${DEFAULT_SEARCH_LIMIT};`
  ].join(" ");
}

export function buildExactSearchGamesQuery(query, platformIds = []) {
  const escapedQuery = escapeApicalypseString(query);
  const platformFilter = platformIds.length > 0 ? ` & platforms = (${platformIds.join(",")})` : "";

  return [
    "fields id,name,cover.url,cover.image_id,platforms.name,platforms.slug,total_rating_count,hypes,first_release_date,version_parent,parent_game,category;",
    `where name = "${escapedQuery}" & version_parent = null & parent_game = null & (category = 0 | category = null)${platformFilter};`,
    `limit ${DEFAULT_SEARCH_LIMIT};`
  ].join(" ");
}

export function buildSlugSearchGamesQuery(query, platformIds = []) {
  const slug = slugFromSearchQuery(query);
  const platformFilter = platformIds.length > 0 ? ` & platforms = (${platformIds.join(",")})` : "";

  return [
    "fields id,name,cover.url,cover.image_id,platforms.name,platforms.slug,total_rating_count,hypes,first_release_date,version_parent,parent_game,category;",
    `where slug = "${slug}" & version_parent = null & parent_game = null & (category = 0 | category = null)${platformFilter};`,
    `limit ${DEFAULT_SEARCH_LIMIT};`
  ].join(" ");
}

export function buildExploreGamesQueries({ platformIds, now, recommendationGenreIds = [], indieGenreIds = [] }) {
  const platformFilter = `platforms = (${platformIds.join(",")})`;
  const baseWhere = [
    platformFilter,
    "version_parent = null",
    "parent_game = null",
    "(category = 0 | category = null)"
  ].join(" & ");
  const todayEpoch = Math.floor(now.getTime() / 1000);
  const recentFromEpoch = todayEpoch - 365 * 24 * 60 * 60;

  return {
    topSellers: buildTopSellersTrendingQuery({ baseWhere, fromEpoch: recentFromEpoch, toEpoch: todayEpoch }),
    recommended: recommendationGenreIds.length > 0
      ? buildGenreRecommendedQuery({ baseWhere, genreIds: recommendationGenreIds })
      : buildRecommendedFallbackQuery({ platformIds }),
    newReleases: [
      EXPLORE_GAME_FIELDS,
      `where ${baseWhere} & first_release_date >= ${recentFromEpoch} & first_release_date <= ${todayEpoch};`,
      "sort first_release_date desc;",
      `limit ${DEFAULT_EXPLORE_LIMIT};`
    ].join(" "),
    qualityTime: buildIndieHighlightsQuery({
      baseWhere,
      genreIds: indieGenreIds,
      toEpoch: todayEpoch
    })
  };
}

export function buildIndieHighlightsQuery({ baseWhere, genreIds = [], toEpoch }) {
  const indieFilter = genreIds.length > 0 ? ` & genres = (${genreIds.join(",")})` : "";

  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere}${indieFilter} & first_release_date <= ${toEpoch} & total_rating >= 75 & total_rating_count > 10;`,
    "sort total_rating desc;",
    `limit ${DEFAULT_EXPLORE_LIMIT};`
  ].join(" ");
}

export function buildTopSellersTrendingQuery({ baseWhere, fromEpoch, toEpoch }) {
  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere} & first_release_date >= ${fromEpoch} & first_release_date < ${toEpoch} & hypes > 0;`,
    "sort hypes desc;",
    `limit ${DEFAULT_EXPLORE_LIMIT};`
  ].join(" ");
}

export function buildTopSellersFallbackQuery({ platformIds, now }) {
  const platformFilter = `platforms = (${platformIds.join(",")})`;
  const baseWhere = [
    platformFilter,
    "version_parent = null",
    "parent_game = null",
    "(category = 0 | category = null)"
  ].join(" & ");
  const todayEpoch = Math.floor(now.getTime() / 1000);
  const recentFromEpoch = todayEpoch - 365 * 24 * 60 * 60;

  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere} & first_release_date >= ${recentFromEpoch} & first_release_date < ${todayEpoch} & total_rating_count > 5;`,
    "sort total_rating_count desc;",
    `limit ${DEFAULT_EXPLORE_LIMIT};`
  ].join(" ");
}

export function buildGenreRecommendedQuery({ baseWhere, genreIds }) {
  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere} & genres = (${genreIds.join(",")}) & total_rating_count > 5;`,
    "sort total_rating_count desc;",
    `limit ${DEFAULT_EXPLORE_LIMIT};`
  ].join(" ");
}

export function buildCategoryGamesQuery({ platformIds, genreIds = [], themeIds = [] }) {
  const platformFilter = `platforms = (${platformIds.join(",")})`;
  const categoryFilters = [
    genreIds.length > 0 ? `genres = (${genreIds.join(",")})` : null,
    themeIds.length > 0 ? `themes = (${themeIds.join(",")})` : null
  ].filter(Boolean);
  const baseWhere = [
    platformFilter,
    "version_parent = null",
    "parent_game = null",
    "(category = 0 | category = null)",
    categoryFilters.length === 1 ? categoryFilters[0] : `(${categoryFilters.join(" | ")})`
  ].join(" & ");

  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere};`,
    "sort total_rating_count desc;",
    `limit ${DEFAULT_CATEGORY_LIMIT};`
  ].join(" ");
}

export function buildRecommendedFallbackQuery({ platformIds }) {
  const platformFilter = `platforms = (${platformIds.join(",")})`;
  const baseWhere = [
    platformFilter,
    "version_parent = null",
    "parent_game = null",
    "(category = 0 | category = null)"
  ].join(" & ");

  return [
    EXPLORE_GAME_FIELDS,
    `where ${baseWhere} & hypes > 0;`,
    "sort hypes desc;",
    `limit ${DEFAULT_EXPLORE_LIMIT};`
  ].join(" ");
}

export function buildGameDetailsQuery(gameId) {
  return [
    "fields id,name,cover.url,cover.image_id,platforms.name,summary,artworks.url,artworks.image_id,screenshots.url,screenshots.image_id,genres.name,involved_companies.company.name,involved_companies.developer,first_release_date,age_ratings.category,age_ratings.rating,age_ratings.rating_cover_url,age_ratings.rating_category.rating,external_games.category,external_games.uid,external_games.url,websites.category,websites.url;",
    `where id = ${gameId};`,
    "limit 1;"
  ].join(" ");
}

export function normalizeExploreGames(rawGames) {
  if (!Array.isArray(rawGames)) {
    return [];
  }

  const gamesById = new Map();

  rawGames.forEach((game) => {
    const normalized = normalizeExploreGame(game);
    if (!normalized) {
      return;
    }

    const previous = gamesById.get(normalized.id);
    if (!previous) {
      gamesById.set(normalized.id, normalized);
      return;
    }

    gamesById.set(normalized.id, {
      ...previous,
      coverUrl: previous.coverUrl || normalized.coverUrl,
      heroImageUrl: previous.heroImageUrl || normalized.heroImageUrl,
      platforms: uniqueStrings([...previous.platforms, ...normalized.platforms])
    });
  });

  return Array.from(gamesById.values());
}

export function normalizeSearchGames(rawGames) {
  if (!Array.isArray(rawGames)) {
    return [];
  }

  const gamesById = new Map();

  rawGames.forEach((game) => {
    if (!game || !game.id || !game.name) {
      return;
    }

    if (game.version_parent || game.parent_game) {
      return;
    }

    if (!isMainGameCategory(game.category)) {
      return;
    }

    const platforms = normalizeSearchPlatformNames(game.platforms);
    if (platforms.length === 0) {
      return;
    }

    const normalized = {
      id: game.id,
      name: game.name,
      coverUrl: imageUrlFromAsset(game.cover, "t_cover_big"),
      platforms
    };

    const previous = gamesById.get(game.id);
    if (!previous) {
      gamesById.set(game.id, normalized);
      return;
    }

    gamesById.set(game.id, {
      ...previous,
      coverUrl: previous.coverUrl || normalized.coverUrl,
      platforms: uniqueStrings([...previous.platforms, ...normalized.platforms])
    });
  });

  return Array.from(gamesById.values());
}

export function syntheticSteamFallbackId(appId) {
  const numericAppId = Number.parseInt(appId, 10);
  return Number.isFinite(numericAppId) && numericAppId > 0
    ? STEAM_FALLBACK_ID_OFFSET + numericAppId
    : null;
}

export function buildExploreGamesPayload(sections, now, recommendationGenres = []) {
  const usedIds = new Set();
  const takeUnique = (games, limit, options = {}) => {
    const selected = [];
    normalizeExploreGames(games).forEach((game) => {
      if (selected.length >= limit) {
        return;
      }
      if (options.requireReleaseDateBeforeToday && !isReleasedBefore(game, now)) {
        return;
      }
      if (usedIds.has(game.id)) {
        return;
      }
      usedIds.add(game.id);
      selected.push(game);
    });
    return selected;
  };

  return {
    generatedAt: now.toISOString(),
    topSellers: takeUnique(sections.topSellers, 10, { requireReleaseDateBeforeToday: true }),
    recommended: takeUnique(sections.recommended, 6),
    newReleases: takeUnique(sections.newReleases, 10),
    qualityTime: takeUnique(sections.qualityTime, 5),
    recommendationMessage: recommendationGenres.length > 0
      ? `Recomendaciones basadas en tus generos: ${recommendationGenres.join(", ")}.`
      : "Seleccion de IGDB para PC, PlayStation y Xbox."
  };
}

export function normalizeExploreReleasedGames(rawGames, now) {
  return normalizeExploreGames(rawGames).filter((game) => isReleasedBefore(game, now));
}

export function normalizeGameDetails(rawGame) {
  if (!rawGame || !rawGame.id || !rawGame.name) {
    return null;
  }

  const screenshots = [
    ...normalizeImageAssets(rawGame.artworks, "t_screenshot_big"),
    ...normalizeImageAssets(rawGame.screenshots, "t_screenshot_big")
  ];
  const coverUrl = imageUrlFromAsset(rawGame.cover, "t_cover_big");

  return {
    id: rawGame.id,
    name: rawGame.name,
    coverUrl,
    heroImageUrl: screenshots[0] || coverUrl,
    summary: nonBlankOrNull(rawGame.summary),
    platforms: normalizePlatformNames(rawGame.platforms),
    developers: normalizeDevelopers(rawGame.involved_companies),
    genres: normalizeNames(rawGame.genres),
    releaseDate: unixSecondsToIso(rawGame.first_release_date),
    screenshots: uniqueStrings(screenshots),
    ageRating: normalizePegiAgeRating(rawGame.age_ratings),
    steamAppId: normalizeSteamAppId(rawGame)
  };
}

export function normalizePegiAgeRating(ageRatings) {
  if (!Array.isArray(ageRatings)) {
    return null;
  }

  const pegi = ageRatings.find(isPegiAgeRating);
  if (!pegi) {
    return null;
  }

  const minimumAge = pegiMinimumAge(pegi.rating_category?.rating) ?? pegiMinimumAge(pegi.rating);
  if (!minimumAge) {
    return null;
  }

  const label = `PEGI ${minimumAge}`;
  return {
    label,
    minimumAge,
    imageUrl: nonBlankOrNull(pegi.rating_cover_url)
  };
}

export function normalizeSteamAppId(rawGame) {
  const directSteamAppId = rawGame?.external?.steam;
  if (Number.isFinite(directSteamAppId)) {
    return String(Math.trunc(directSteamAppId));
  }
  if (typeof directSteamAppId === "string" && /^\d+$/.test(directSteamAppId.trim())) {
    return directSteamAppId.trim();
  }

  const externalGames = rawGame?.external_games;
  if (!Array.isArray(externalGames)) {
    return normalizeSteamAppIdFromWebsites(rawGame?.websites);
  }

  const steamGame = externalGames.find((externalGame) => {
    const category = externalGame?.category;
    return category === 1 || String(category?.name || category).toLowerCase() === "steam";
  });
  const uid = typeof steamGame?.uid === "string" ? steamGame.uid.trim() : "";
  if (/^\d+$/.test(uid)) {
    return uid;
  }

  const url = typeof steamGame?.url === "string" ? steamGame.url : "";
  return url.match(/store\.steampowered\.com\/app\/(\d+)/i)?.[1] ??
    normalizeSteamAppIdFromWebsites(rawGame?.websites);
}

function normalizeSteamAppIdFromWebsites(websites) {
  if (!Array.isArray(websites)) {
    return null;
  }

  const steamWebsite = websites.find((website) => {
    const category = website?.category;
    const url = typeof website?.url === "string" ? website.url : "";
    return category === 13 ||
      String(category?.name || category).toLowerCase() === "steam" ||
      /store\.steampowered\.com\/app\/\d+/i.test(url);
  });

  const url = typeof steamWebsite?.url === "string" ? steamWebsite.url : "";
  return url.match(/store\.steampowered\.com\/app\/(\d+)/i)?.[1] ?? null;
}

async function getSteamSearchFallbackGames(query, fetchFn) {
  const response = await fetchFn(
    `https://store.steampowered.com/api/storesearch/?term=${encodeURIComponent(query)}&l=spanish&cc=ES`
  ).catch(() => null);
  if (!response?.ok) {
    return [];
  }

  const payload = await response.json().catch(() => null);
  const normalizedQuery = query.toLowerCase();
  const items = Array.isArray(payload?.items) ? payload.items : [];

  return items
    .filter((item) => item?.type === "app" || item?.type === undefined)
    .filter((item) => typeof item?.name === "string" && item.name.trim() !== "")
    .filter((item) => {
      const name = item.name.trim().toLowerCase();
      return name === normalizedQuery || name.includes(normalizedQuery);
    })
    .slice(0, 3)
    .map((item) => {
      const id = syntheticSteamFallbackId(item.id);
      if (!id) return null;
      return {
        id,
        name: item.name.trim(),
        coverUrl: nonBlankOrNull(item.tiny_image),
        platforms: ["PC (Microsoft Windows)"]
      };
    })
    .filter(Boolean);
}

async function getSteamFallbackGameDetailsPayload({ syntheticId, fetchFn }) {
  const appId = syntheticId - STEAM_FALLBACK_ID_OFFSET;
  const response = await fetchFn(
    `https://store.steampowered.com/api/appdetails?appids=${appId}&l=spanish&cc=ES`
  );
  if (!response.ok) {
    throw publicError(502, "Steam no ha respondido correctamente.");
  }

  const payload = await response.json();
  const details = payload?.[String(appId)]?.success ? payload[String(appId)]?.data : null;
  if (!details) {
    throw publicError(404, "Steam no ha encontrado ese juego.");
  }

  const screenshots = Array.isArray(details.screenshots)
    ? details.screenshots.map((screenshot) => nonBlankOrNull(screenshot?.path_full)).filter(Boolean)
    : [];

  return {
    id: syntheticId,
    name: details.name || `Steam App ${appId}`,
    coverUrl: nonBlankOrNull(details.header_image),
    heroImageUrl: nonBlankOrNull(details.background_raw) || nonBlankOrNull(details.background) || nonBlankOrNull(details.header_image),
    summary: nonBlankOrNull(details.short_description),
    platforms: ["PC (Microsoft Windows)"],
    developers: Array.isArray(details.developers) ? uniqueStrings(details.developers.filter(Boolean)) : [],
    genres: Array.isArray(details.genres)
      ? uniqueStrings(details.genres.map((genre) => genre?.description).filter(Boolean))
      : [],
    releaseDate: nonBlankOrNull(details.release_date?.date),
    screenshots: uniqueStrings(screenshots),
    ageRating: normalizeSteamPegiAgeRating(details),
    steamAppId: String(appId)
  };
}

function isSteamFallbackId(id) {
  return id > STEAM_FALLBACK_ID_OFFSET;
}

async function resolveExplorePlatformIds(fetchFn, clientId, token, nowMs) {
  if (explorePlatformCache.ids && nowMs < explorePlatformCache.expiresAt) {
    return explorePlatformCache.ids;
  }

  const query = [
    "fields id,name,slug;",
    `where slug = (${EXPLORE_PLATFORM_SLUGS.map((slug) => `"${slug}"`).join(",")});`,
    "limit 10;"
  ].join(" ");
  const response = await postIgdb(fetchFn, "platforms", query, clientId, token);
  const platforms = await response.json();
  const ids = platforms
    .filter((platform) => EXPLORE_PLATFORM_SLUGS.includes(platform.slug))
    .map((platform) => platform.id);

  if (ids.length === 0) {
    throw publicError(502, "IGDB no ha devuelto las plataformas configuradas para Explorar.");
  }

  explorePlatformCache.ids = ids;
  explorePlatformCache.expiresAt = nowMs + 24 * 60 * 60 * 1000;
  return ids;
}

async function resolveGenreIds(fetchFn, clientId, token, genreNames, nowMs) {
  return resolveNamedIds({
    fetchFn,
    clientId,
    token,
    names: genreNames,
    nowMs,
    endpoint: "genres",
    cache: genreCache
  });
}

async function resolveThemeIds(fetchFn, clientId, token, themeNames, nowMs) {
  return resolveNamedIds({
    fetchFn,
    clientId,
    token,
    names: themeNames,
    nowMs,
    endpoint: "themes",
    cache: themeCache
  });
}

async function resolveNamedIds({ fetchFn, clientId, token, names, nowMs, endpoint, cache }) {
  if (cache.expiresAt <= nowMs) {
    cache.idsByName = new Map();
    cache.expiresAt = nowMs + 24 * 60 * 60 * 1000;
  }

  const missingGenres = names.filter((name) => !cache.idsByName.has(name.toLowerCase()));
  if (missingGenres.length > 0) {
    const responses = await Promise.all(
      missingGenres.map(async (name) => {
        const response = await postIgdb(fetchFn, endpoint, buildGenreLookupQuery(name), clientId, token);
        return response.json();
      })
    );
    responses.flat().forEach((genre) => {
      if (Number.isFinite(genre?.id) && typeof genre?.name === "string") {
        cache.idsByName.set(genre.name.trim().toLowerCase(), genre.id);
      }
    });
    missingGenres.forEach((name) => {
      cache.idsByName.set(name.toLowerCase(), cache.idsByName.get(name.toLowerCase()) ?? null);
    });
  }

  return names
    .map((name) => cache.idsByName.get(name.toLowerCase()))
    .filter((id) => Number.isFinite(id));
}

export function buildGenreLookupQuery(name) {
  return [
    "fields id,name;",
    `where name = "${escapeApicalypseString(name)}";`,
    "limit 1;"
  ].join(" ");
}

function normalizeExploreGame(rawGame) {
  const name = typeof rawGame?.name === "string" ? rawGame.name.trim() : "";
  if (!rawGame || !rawGame.id || !name || rawGame.version_parent || rawGame.parent_game) {
    return null;
  }

  const platforms = normalizeExplorePlatformNames(rawGame.platforms);
  if (platforms.length === 0) {
    return null;
  }

  const screenshots = [
    ...normalizeImageAssets(rawGame.artworks, "t_screenshot_big"),
    ...normalizeImageAssets(rawGame.screenshots, "t_screenshot_big")
  ];
  const coverUrl = imageUrlFromAsset(rawGame.cover, "t_cover_big");

  return {
    id: rawGame.id,
    name,
    coverUrl,
    heroImageUrl: screenshots[0] || coverUrl,
    summary: nonBlankOrNull(rawGame.summary),
    platforms,
    developers: normalizeDevelopers(rawGame.involved_companies),
    genres: normalizeNames(rawGame.genres),
    releaseDate: unixSecondsToIso(rawGame.first_release_date),
    rating: roundedNumber(rawGame.total_rating ?? rawGame.rating ?? rawGame.aggregated_rating),
    ratingCount: integerOrNull(rawGame.total_rating_count ?? rawGame.rating_count ?? rawGame.aggregated_rating_count),
    hypes: integerOrNull(rawGame.hypes)
  };
}

function normalizePlatformNames(platforms) {
  return normalizeNames(platforms);
}

function normalizeSearchPlatformNames(platforms) {
  if (!Array.isArray(platforms)) {
    return [];
  }

  return uniqueStrings(
    platforms
      .filter(isSupportedSearchPlatform)
      .map((platform) => platform?.name)
      .filter((name) => typeof name === "string" && name.trim() !== "")
  );
}

function mergeSearchGames(games) {
  const gamesById = new Map();
  games.forEach((game) => {
    if (!game || !game.id || !game.name) return;
    if (!gamesById.has(game.id)) {
      gamesById.set(game.id, game);
    }
  });
  return Array.from(gamesById.values()).slice(0, DEFAULT_SEARCH_LIMIT);
}

function normalizeExplorePlatformNames(platforms) {
  if (!Array.isArray(platforms)) {
    return [];
  }

  return uniqueStrings(
    platforms
      .filter((platform) => EXPLORE_PLATFORM_SLUGS.includes(platform?.slug))
      .map((platform) => platform?.name)
      .filter((name) => typeof name === "string" && name.trim() !== "")
  );
}

function isSupportedSearchPlatform(platform) {
  const slug = typeof platform?.slug === "string" ? platform.slug : "";
  if (EXPLORE_PLATFORM_SLUGS.includes(slug)) {
    return true;
  }

  const name = typeof platform?.name === "string" ? platform.name.toLowerCase() : "";
  return name.includes("pc ") ||
    name.includes("microsoft windows") ||
    name.includes("playstation") ||
    name.includes("xbox");
}

function isMainGameCategory(category) {
  return category === undefined || category === null || category === 0;
}

function normalizeNames(items) {
  if (!Array.isArray(items)) {
    return [];
  }

  return uniqueStrings(
    items
      .map((item) => item?.name)
      .filter((name) => typeof name === "string" && name.trim() !== "")
  );
}

function normalizeDevelopers(companies) {
  if (!Array.isArray(companies)) {
    return [];
  }

  return uniqueStrings(
    companies
      .filter((company) => company?.developer === true)
      .map((company) => company?.company?.name)
      .filter((name) => typeof name === "string" && name.trim() !== "")
  );
}

function isPegiAgeRating(ageRating) {
  const category = ageRating?.category;
  const organization = ageRating?.organization;
  return category === 2 ||
    String(category?.name || category).toLowerCase() === "pegi" ||
    String(organization?.name || organization).toLowerCase() === "pegi";
}

function pegiMinimumAge(value) {
  if (typeof value === "number") {
    return {
      1: 3,
      2: 7,
      3: 12,
      4: 16,
      5: 18
    }[value] ?? null;
  }

  if (typeof value === "string") {
    const match = value.match(/(?:PEGI[_\s-]?)?(\d{1,2})/i);
    const age = match ? Number.parseInt(match[1], 10) : Number.NaN;
    return [3, 7, 12, 16, 18].includes(age) ? age : null;
  }

  return null;
}

function normalizeSteamPegiAgeRating(details) {
  const pegiAge = steamAgeFromValue(details?.ratings?.pegi?.rating);
  if (pegiAge) {
    return {
      label: `PEGI ${pegiAge}`,
      minimumAge: pegiAge,
      imageUrl: null
    };
  }

  const requiredAge = steamAgeFromValue(details?.required_age);
  if (requiredAge && requiredAge > 0 && requiredAge <= 21) {
    return {
      label: `+${requiredAge}`,
      minimumAge: requiredAge,
      imageUrl: null
    };
  }

  return null;
}

function steamAgeFromValue(value) {
  if (Number.isFinite(value)) {
    return Math.trunc(value);
  }
  if (typeof value !== "string") {
    return null;
  }
  const match = value.match(/\d{1,2}/);
  return match ? Number.parseInt(match[0], 10) : null;
}

function normalizeImageAssets(assets, size) {
  if (!Array.isArray(assets)) {
    return [];
  }

  return uniqueStrings(
    assets
      .map((asset) => imageUrlFromAsset(asset, size))
      .filter(Boolean)
  );
}

function uniqueStrings(values) {
  return Array.from(new Set(values));
}

function isReleasedBefore(game, now) {
  if (!game.releaseDate) {
    return false;
  }

  return game.releaseDate < now.toISOString().slice(0, 10);
}

function normalizeSearchQuery(query) {
  if (typeof query !== "string") {
    return "";
  }

  return query.trim().replace(/\s+/g, " ");
}

function slugFromSearchQuery(query) {
  return query
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function normalizeExploreGenreNames(genres) {
  const values = Array.isArray(genres)
    ? genres
    : typeof genres === "string"
      ? genres.split(",")
      : [];
  const genresByKey = new Map();

  values.forEach((value) => {
    if (typeof value !== "string") return;
    const normalized = value.trim().replace(/\s+/g, " ");
    if (!normalized) return;
    const key = normalized.toLowerCase();
    if (!genresByKey.has(key)) {
      genresByKey.set(key, normalized);
    }
  });

  return Array.from(genresByKey.values()).slice(0, 5);
}

export function normalizeCategoryGenreName(genre) {
  if (typeof genre !== "string") {
    return "";
  }

  return genre.trim().replace(/\s+/g, " ");
}

function escapeApicalypseString(value) {
  return value.replace(/\\/g, "\\\\").replace(/"/g, "\\\"");
}

function unixSecondsToIso(value) {
  if (!Number.isFinite(value)) return null;
  return new Date(value * 1000).toISOString().slice(0, 10);
}

function nonBlankOrNull(value) {
  return typeof value === "string" && value.trim() !== "" ? value.trim() : null;
}

function roundedNumber(value) {
  return Number.isFinite(value) ? Math.round(value * 10) / 10 : null;
}

function integerOrNull(value) {
  return Number.isFinite(value) ? Math.trunc(value) : null;
}

const EXPLORE_GAME_FIELDS = [
  "fields id,name,cover.url,cover.image_id,platforms.name,platforms.slug,summary,artworks.url,artworks.image_id,screenshots.url,screenshots.image_id,genres.name,involved_companies.company.name,involved_companies.developer,first_release_date,total_rating,total_rating_count,rating,rating_count,aggregated_rating,aggregated_rating_count,hypes,version_parent,parent_game,category;"
].join(" ");
