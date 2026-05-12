import assert from "node:assert/strict";
import test from "node:test";
import {
  buildCategoryGamesQuery,
  buildExactSearchGamesQuery,
  buildExploreGamesPayload,
  buildExploreGamesQueries,
  buildGenreRecommendedQuery,
  buildGenreLookupQuery,
  buildGameDetailsQuery,
  buildSearchGamesQuery,
  buildSlugSearchGamesQuery,
  buildTopSellersFallbackQuery,
  normalizeGameDetails,
  normalizeCategoryGenreName,
  normalizeExploreGames,
  normalizeExploreGenreNames,
  normalizeExploreReleasedGames,
  normalizePegiAgeRating,
  normalizeSearchGames,
  syntheticSteamFallbackId
} from "../src/igdbGames.js";
import { normalizeIgdbImageUrl } from "../src/igdbClient.js";

test("buildSearchGamesQuery filters versioned duplicates", () => {
  const query = buildSearchGamesQuery("elden ring", [6, 48, 167, 49, 169]);

  assert.match(query, /search "elden ring"/);
  assert.match(query, /platforms = \(6,48,167,49,169\)/);
  assert.match(query, /version_parent = null/);
  assert.match(query, /parent_game = null/);
  assert.match(query, /\(category = 0 \| category = null\)/);
  assert.match(query, /limit 25/);
});

test("buildExactSearchGamesQuery searches supported main games by exact name", () => {
  const query = buildExactSearchGamesQuery("tekken 7", [6, 48, 167, 49, 169]);

  assert.match(query, /where name = "tekken 7"/);
  assert.match(query, /platforms = \(6,48,167,49,169\)/);
  assert.match(query, /version_parent = null/);
  assert.match(query, /parent_game = null/);
  assert.match(query, /\(category = 0 \| category = null\)/);
});

test("buildSlugSearchGamesQuery searches supported main games by generated slug", () => {
  const query = buildSlugSearchGamesQuery("Tekken 7", [6, 48, 167, 49, 169]);

  assert.match(query, /where slug = "tekken-7"/);
  assert.match(query, /platforms = \(6,48,167,49,169\)/);
  assert.match(query, /\(category = 0 \| category = null\)/);
});

test("buildGameDetailsQuery asks IGDB for detail fields", () => {
  const query = buildGameDetailsQuery(12020);

  assert.match(query, /where id = 12020/);
  assert.match(query, /platforms\.name/);
  assert.match(query, /screenshots\./);
  assert.match(query, /involved_companies\./);
  assert.match(query, /age_ratings\.rating_cover_url/);
  assert.match(query, /external_games\.uid/);
  assert.match(query, /websites\.url/);
});

test("buildExploreGamesQueries filters to PC, PlayStation and Xbox platforms", () => {
  const queries = buildExploreGamesQueries({
    platformIds: [6, 48, 167, 49, 169],
    now: new Date("2026-04-29T12:00:00.000Z"),
    indieGenreIds: [32]
  });

  Object.values(queries).forEach((query) => {
    assert.match(query, /platforms = \(6,48,167,49,169\)/);
    assert.doesNotMatch(query, /switch|nintendo/i);
    assert.match(query, /version_parent = null/);
    assert.match(query, /parent_game = null/);
  });
  assert.match(queries.topSellers, /first_release_date >= 1745928000/);
  assert.match(queries.topSellers, /first_release_date < 1777464000/);
  assert.match(queries.topSellers, /hypes > 0/);
  assert.match(queries.topSellers, /sort hypes desc/);
  assert.doesNotMatch(queries.topSellers, /first_release_date <=/);
  assert.match(queries.newReleases, /sort first_release_date desc/);
  assert.match(queries.qualityTime, /genres = \(32\)/);
  assert.match(queries.qualityTime, /first_release_date <= 1777464000/);
  assert.match(queries.qualityTime, /total_rating >= 75/);
  assert.match(queries.qualityTime, /total_rating_count > 10/);
  assert.match(queries.qualityTime, /sort total_rating desc/);
});

test("buildTopSellersFallbackQuery keeps recent released games only", () => {
  const query = buildTopSellersFallbackQuery({
    platformIds: [6, 48],
    now: new Date("2026-04-29T12:00:00.000Z")
  });

  assert.match(query, /first_release_date >= 1745928000/);
  assert.match(query, /first_release_date < 1777464000/);
  assert.match(query, /total_rating_count > 5/);
  assert.match(query, /sort total_rating_count desc/);
  assert.doesNotMatch(query, /first_release_date <=/);
});

test("buildExploreGamesQueries can personalize recommendations by genre ids", () => {
  const queries = buildExploreGamesQueries({
    platformIds: [6, 48, 167, 49, 169],
    recommendationGenreIds: [31, 32],
    now: new Date("2026-04-29T12:00:00.000Z")
  });

  assert.match(queries.recommended, /genres = \(31,32\)/);
  assert.match(queries.recommended, /sort total_rating_count desc/);
  assert.doesNotMatch(queries.recommended, /hypes > 0/);
});

test("buildGenreRecommendedQuery applies only the provided genre ids", () => {
  const query = buildGenreRecommendedQuery({
    baseWhere: "platforms = (6)",
    genreIds: [12]
  });

  assert.match(query, /where platforms = \(6\) & genres = \(12\)/);
  assert.match(query, /total_rating_count > 5/);
});

test("buildGenreLookupQuery resolves a genre by exact name", () => {
  const query = buildGenreLookupQuery("Role-playing (RPG)");

  assert.match(query, /where name = "Role-playing \(RPG\)"/);
  assert.match(query, /limit 1/);
});

test("buildCategoryGamesQuery filters by genre and configured platforms", () => {
  const query = buildCategoryGamesQuery({
    platformIds: [6, 48, 167, 49, 169],
    genreIds: [31],
    themeIds: [1]
  });

  assert.match(query, /platforms = \(6,48,167,49,169\)/);
  assert.match(query, /genres = \(31\)/);
  assert.match(query, /themes = \(1\)/);
  assert.match(query, /version_parent = null/);
  assert.match(query, /parent_game = null/);
  assert.match(query, /sort total_rating_count desc/);
  assert.match(query, /limit 30/);
});

test("normalizeExploreGenreNames trims and deduplicates genre filters", () => {
  assert.deepEqual(
    normalizeExploreGenreNames(" Action, Adventure, action, Role-playing (RPG), , Shooter "),
    ["Action", "Adventure", "Role-playing (RPG)", "Shooter"]
  );
});

test("normalizeCategoryGenreName trims repeated whitespace", () => {
  assert.equal(normalizeCategoryGenreName("  Role-playing   (RPG) "), "Role-playing (RPG)");
  assert.equal(normalizeCategoryGenreName(null), "");
});

test("normalizeIgdbImageUrl upgrades cover size and adds protocol", () => {
  assert.equal(
    normalizeIgdbImageUrl("//images.igdb.com/igdb/image/upload/t_thumb/cover.png"),
    "https://images.igdb.com/igdb/image/upload/t_cover_big/cover.png"
  );
  assert.equal(normalizeIgdbImageUrl(null), null);
});

test("normalizePegiAgeRating extracts PEGI metadata", () => {
  assert.deepEqual(
    normalizePegiAgeRating([
      { category: 1, rating: 11 },
      {
        category: 2,
        rating: 5,
        rating_cover_url: "https://images.igdb.com/pegi-18.png",
        rating_category: { rating: "18" }
      }
    ]),
    {
      label: "PEGI 18",
      minimumAge: 18,
      imageUrl: "https://images.igdb.com/pegi-18.png"
    }
  );

  assert.equal(normalizePegiAgeRating([{ category: 1, rating: 11 }]), null);
});

test("normalizeGameDetails extracts PEGI and Steam AppID", () => {
  const detail = normalizeGameDetails({
    id: 12020,
    name: "Portal 2",
    cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/portal.jpg" },
    platforms: [{ name: "PC (Microsoft Windows)" }],
    age_ratings: [
      {
        category: 2,
        rating: 3,
        rating_cover_url: "https://images.igdb.com/pegi-12.png"
      }
    ],
    external_games: [
      { category: 1, uid: "620" }
    ]
  });

  assert.equal(detail.ageRating.label, "PEGI 12");
  assert.equal(detail.ageRating.minimumAge, 12);
  assert.equal(detail.steamAppId, "620");
});

test("normalizeGameDetails extracts Steam AppID from external URL", () => {
  const detail = normalizeGameDetails({
    id: 99,
    name: "Steam URL Game",
    external_games: [
      { category: 1, url: "https://store.steampowered.com/app/12345/Game/" }
    ]
  });

  assert.equal(detail.steamAppId, "12345");
});

test("normalizeGameDetails extracts Steam AppID from website URL", () => {
  const detail = normalizeGameDetails({
    id: 100,
    name: "Steam Website Game",
    websites: [
      { category: 13, url: "https://store.steampowered.com/app/620/Portal_2/" }
    ]
  });

  assert.equal(detail.steamAppId, "620");
});

test("normalizeExploreGames keeps allowed platform names and removes duplicates", () => {
  const games = normalizeExploreGames([
    {
      id: 9,
      name: "Shared Adventure",
      cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/shared.png" },
      artworks: [{ url: "//images.igdb.com/igdb/image/upload/t_thumb/art.png" }],
      platforms: [
        { name: "PC (Microsoft Windows)", slug: "win" },
        { name: "Nintendo Switch", slug: "switch" },
        { name: "PlayStation 5", slug: "ps5" }
      ],
      total_rating: 88.44,
      total_rating_count: 120,
      hypes: 10
    },
    {
      id: 9,
      name: "Shared Adventure",
      platforms: [{ name: "Xbox Series X|S", slug: "series-x-s" }]
    },
    {
      id: 10,
      name: "Switch Only",
      platforms: [{ name: "Nintendo Switch", slug: "switch" }]
    }
  ]);

  assert.equal(games.length, 1);
  assert.deepEqual(games[0].platforms, [
    "PC (Microsoft Windows)",
    "PlayStation 5",
    "Xbox Series X|S"
  ]);
  assert.equal(games[0].coverUrl, "https://images.igdb.com/igdb/image/upload/t_cover_big/shared.png");
  assert.equal(games[0].heroImageUrl, "https://images.igdb.com/igdb/image/upload/t_screenshot_big/art.png");
  assert.equal(games[0].rating, 88.4);
  assert.equal(games[0].ratingCount, 120);
  assert.equal(games[0].hypes, 10);
});

test("buildExploreGamesPayload removes games repeated across sections", () => {
  const game = (id, name) => ({
    id,
    name,
    first_release_date: 1777377600,
    platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
  });
  const payload = buildExploreGamesPayload(
    {
      topSellers: [game(1, "First")],
      recommended: [game(1, "First"), game(2, "Second")],
      newReleases: [game(2, "Second"), game(3, "Third")],
      qualityTime: [game(3, "Third"), game(4, "Fourth")]
    },
    new Date("2026-04-29T12:00:00.000Z")
  );

  assert.deepEqual(payload.topSellers.map((item) => item.id), [1]);
  assert.deepEqual(payload.recommended.map((item) => item.id), [2]);
  assert.deepEqual(payload.newReleases.map((item) => item.id), [3]);
  assert.deepEqual(payload.qualityTime.map((item) => item.id), [4]);
});

test("buildExploreGamesPayload excludes unreleased and undated top sellers", () => {
  const game = (id, name, firstReleaseDate) => ({
    id,
    name,
    first_release_date: firstReleaseDate,
    platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
  });
  const payload = buildExploreGamesPayload(
    {
      topSellers: [
        game(1, "Released Yesterday", 1777377600),
        game(2, "Releases Today", 1777464000),
        game(3, "Undated", undefined),
        game(4, "Future", 1777550400)
      ],
      recommended: [],
      newReleases: [],
      qualityTime: []
    },
    new Date("2026-04-29T12:00:00.000Z")
  );

  assert.deepEqual(payload.topSellers.map((item) => item.name), ["Released Yesterday"]);
});

test("normalizeExploreReleasedGames keeps only games before today", () => {
  const games = normalizeExploreReleasedGames(
    [
      {
        id: 1,
        name: "Past",
        first_release_date: 1777377600,
        platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
      },
      {
        id: 2,
        name: "Today",
        first_release_date: 1777464000,
        platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
      },
      {
        id: 3,
        name: "No Date",
        platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
      }
    ],
    new Date("2026-04-29T12:00:00.000Z")
  );

  assert.deepEqual(games.map((game) => game.name), ["Past"]);
});

test("buildExploreGamesPayload explains personalized genre recommendations", () => {
  const payload = buildExploreGamesPayload(
    {
      topSellers: [],
      recommended: [],
      newReleases: [],
      qualityTime: []
    },
    new Date("2026-04-29T12:00:00.000Z"),
    ["Action", "Adventure"]
  );

  assert.equal(payload.recommendationMessage, "Recomendaciones basadas en tus generos: Action, Adventure.");
});

test("normalizeSearchGames maps cover and platforms and skips child versions", () => {
  const games = normalizeSearchGames([
    {
      id: 1,
      name: "Main Game",
      cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/main.png" },
      platforms: [
        { name: "PC (Microsoft Windows)", slug: "win" },
        { name: "PlayStation 5", slug: "ps5" }
      ],
      category: 0
    },
    {
      id: 2,
      name: "Main Game Deluxe",
      version_parent: 1,
      platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }]
    }
  ]);

  assert.equal(games.length, 1);
  assert.deepEqual(games[0], {
    id: 1,
    name: "Main Game",
    coverUrl: "https://images.igdb.com/igdb/image/upload/t_cover_big/main.png",
    platforms: ["PC (Microsoft Windows)", "PlayStation 5"]
  });
});

test("normalizeSearchGames skips unsupported platform results", () => {
  const games = normalizeSearchGames([
    {
      id: 394038,
      name: "Tekken 7",
      platforms: [{ name: "Arcade", slug: "arcade" }],
      category: 0,
      total_rating_count: 1
    },
    {
      id: 17,
      name: "Tekken 7",
      platforms: [
        { name: "PC (Microsoft Windows)", slug: "win" },
        { name: "PlayStation 4", slug: "ps4--1" }
      ],
      category: 0,
      total_rating_count: 200
    },
    {
      id: 18,
      name: "Tekken 7 Season Pass",
      platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }],
      category: 1
    }
  ]);

  assert.deepEqual(games.map((game) => game.id), [17]);
  assert.deepEqual(games[0].platforms, ["PC (Microsoft Windows)", "PlayStation 4"]);
});

test("syntheticSteamFallbackId creates stable positive ids", () => {
  assert.equal(syntheticSteamFallbackId("389730"), 800000389730);
  assert.equal(syntheticSteamFallbackId("not-a-number"), null);
});

test("normalizeGameDetails extracts human friendly detail data", () => {
  const detail = normalizeGameDetails({
    id: 99,
    name: "Future Adventure",
    cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/cover.png" },
    artworks: [{ url: "//images.igdb.com/igdb/image/upload/t_thumb/art.png" }],
    screenshots: [{ url: "//images.igdb.com/igdb/image/upload/t_thumb/screen.png" }],
    platforms: [{ name: "PC (Microsoft Windows)" }, { name: "Xbox Series X|S" }],
    summary: "  A stylish adventure. ",
    involved_companies: [
      { developer: true, company: { name: "Studio One" } },
      { developer: false, company: { name: "Publisher Two" } }
    ],
    genres: [{ name: "Adventure" }, { name: "Action" }],
    first_release_date: 1778371200,
    age_ratings: [
      { category: 2, rating: 4 }
    ],
    websites: [
      { category: 13, url: "https://store.steampowered.com/app/555/Future_Adventure/" }
    ]
  });

  assert.deepEqual(detail, {
    id: 99,
    name: "Future Adventure",
    coverUrl: "https://images.igdb.com/igdb/image/upload/t_cover_big/cover.png",
    heroImageUrl: "https://images.igdb.com/igdb/image/upload/t_screenshot_big/art.png",
    summary: "A stylish adventure.",
    platforms: ["PC (Microsoft Windows)", "Xbox Series X|S"],
    developers: ["Studio One"],
    genres: ["Adventure", "Action"],
    releaseDate: "2026-05-10",
    ageRating: {
      label: "PEGI 16",
      minimumAge: 16,
      imageUrl: null
    },
    steamAppId: "555",
    screenshots: [
      "https://images.igdb.com/igdb/image/upload/t_screenshot_big/art.png",
      "https://images.igdb.com/igdb/image/upload/t_screenshot_big/screen.png"
    ]
  });
});
