import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";

const env = {
  TWITCH_CLIENT_ID: "client-id",
  TWITCH_CLIENT_SECRET: "client-secret"
};

test("GET /search-games routes to the IGDB search payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/search-games?q=halo"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(response.headers.get("Cache-Control"), "no-store");
    assert.equal(body.query, "halo");
    assert.deepEqual(body.games, [
      {
        id: 1,
        name: "Halo",
        coverUrl: "https://images.igdb.com/igdb/image/upload/t_cover_big/halo.jpg",
        platforms: ["Xbox"]
      }
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /search-games prioritizes Steam fallback for Tekken 7", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/search-games?q=tekken%207&_cb=test"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(response.headers.get("Cache-Control"), "no-store");
    assert.equal(body.games[0].id, 800000389730);
    assert.equal(body.games[0].name, "TEKKEN 7");
    assert.deepEqual(body.games[0].platforms, ["PC (Microsoft Windows)"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /games/:id routes to the IGDB detail payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/games/123"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(response.headers.get("Cache-Control"), "no-store");
    assert.equal(body.id, 123);
    assert.equal(body.name, "Halo Detail");
    assert.equal(body.coverUrl, "https://images.igdb.com/igdb/image/upload/t_cover_big/halo-detail.jpg");
    assert.deepEqual(body.ageRating, {
      label: "PEGI 16",
      minimumAge: 16,
      imageUrl: "https://images.igdb.com/pegi-16.png"
    });
    assert.equal(body.steamAppId, "12345");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /games/:id can return Steam fallback details", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/games/800000389730"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(body.id, 800000389730);
    assert.equal(body.name, "TEKKEN 7");
    assert.equal(body.steamAppId, "389730");
    assert.deepEqual(body.platforms, ["PC (Microsoft Windows)"]);
    assert.deepEqual(body.ageRating, {
      label: "PEGI 16",
      minimumAge: 16,
      imageUrl: null
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /explore-games routes to the IGDB explore payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/explore-games"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(body.topSellers[0].name, "Halo Popular");
    assert.deepEqual(body.topSellers[0].platforms, ["Xbox Series X|S"]);
    assert.equal(body.qualityTime[0].name, "Indie Highlight");
    assert.equal(body.recommendationMessage, "Seleccion de IGDB para PC, PlayStation y Xbox.");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /explore-games sends genre filters to personalized recommendations", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/explore-games?genres=Action,Adventure,Action"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(body.recommended[0].name, "Genre Match");
    assert.equal(body.recommendationMessage, "Recomendaciones basadas en tus generos: Action, Adventure.");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /category-games routes to IGDB category payload", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/category-games?genre=Action"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(body.genre, "Action");
    assert.equal(body.games[0].name, "Action Category Hit");
    assert.deepEqual(body.games[0].platforms, ["PlayStation 5", "Xbox Series X|S"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("GET /release-calendar applies compact limit", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mockIgdbFetch;

  try {
    const response = await worker.fetch(
      new Request("https://example.test/release-calendar?from=2026-05-02&days=60&limit=1"),
      env
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(response.headers.get("Cache-Control"), "public, max-age=1800");
    assert.equal(body.releases.length, 1);
    assert.equal(body.releases[0].name, "Soon First");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("unknown routes still return 404", async () => {
  const response = await worker.fetch(
    new Request("https://example.test/missing"),
    env
  );
  const body = await response.json();

  assert.equal(response.status, 404);
  assert.deepEqual(body, { error: "Not found" });
});

test("GET /psn/status reports NPSSO auth mode", async () => {
  const response = await worker.fetch(
    new Request("https://example.test/psn/status"),
    env
  );
  const body = await response.json();

  assert.equal(response.status, 200);
  assert.equal(body.configured, true);
  assert.equal(body.authMode, "npsso");
});

test("POST /psn/auth/npsso rejects malformed NPSSO before external calls", async () => {
  const response = await worker.fetch(
    new Request("https://example.test/psn/auth/npsso", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ npsso: "short" })
    }),
    env
  );
  const body = await response.json();

  assert.equal(response.status, 400);
  assert.match(body.error, /NPSSO/);
});

async function mockIgdbFetch(url, options = {}) {
  const urlString = url.toString();

  if (urlString.startsWith("https://store.steampowered.com/api/storesearch/?term=tekken%207")) {
    return jsonResponse({
      items: [
        {
          id: 389730,
          type: "app",
          name: "TEKKEN 7",
          tiny_image: "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/389730/capsule_sm_120.jpg"
        }
      ]
    });
  }

  if (urlString === "https://store.steampowered.com/api/appdetails?appids=389730&l=spanish&cc=ES") {
    return jsonResponse({
      389730: {
        success: true,
        data: {
          name: "TEKKEN 7",
          steam_appid: 389730,
          header_image: "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/389730/header.jpg",
          short_description: "La conclusion epica del clan Mishima.",
          developers: ["BANDAI NAMCO Studios Inc."],
          genres: [{ description: "Action" }],
          ratings: { pegi: { rating: "16" } },
          screenshots: [{ path_full: "https://shared.fastly.steamstatic.com/tekken.jpg" }]
        }
      }
    });
  }

  if (urlString.startsWith("https://id.twitch.tv/oauth2/token")) {
    return jsonResponse({ access_token: "token", expires_in: 3600 });
  }

  if (urlString === "https://api.igdb.com/v4/games") {
    const body = options.body || "";
    if (body.includes('where name = "halo"')) {
      return jsonResponse([
        {
          id: 1,
          name: "Halo",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/halo.jpg" },
          platforms: [{ name: "Xbox" }]
        }
      ]);
    }

    if (body.includes('where slug = "halo"')) {
      return jsonResponse([
        {
          id: 1,
          name: "Halo",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/halo.jpg" },
          platforms: [{ name: "Xbox" }]
        }
      ]);
    }

    if (body.includes('search "halo"')) {
      return jsonResponse([
        {
          id: 1,
          name: "Halo",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/halo.jpg" },
          platforms: [{ name: "Xbox" }]
        }
      ]);
    }

    if (body.includes("genres = (31,32)")) {
      return jsonResponse([
        {
          id: 88,
          name: "Genre Match",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/genre-match.jpg" },
          platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }],
          genres: [{ name: "Action" }, { name: "Adventure" }],
          total_rating: 87,
          total_rating_count: 40
        }
      ]);
    }

    if (body.includes("genres = (31)")) {
      return jsonResponse([
        {
          id: 144,
          name: "Action Category Hit",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/action-category.jpg" },
          platforms: [
            { name: "PlayStation 5", slug: "ps5" },
            { name: "Xbox Series X|S", slug: "series-x-s" },
            { name: "Nintendo Switch", slug: "switch" }
          ],
          genres: [{ name: "Action" }],
          total_rating: 91,
          total_rating_count: 200
        }
      ]);
    }

    if (body.includes("genres = (33)")) {
      return jsonResponse([
        {
          id: 155,
          name: "Indie Highlight",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/indie-highlight.jpg" },
          platforms: [{ name: "PC (Microsoft Windows)", slug: "win" }],
          genres: [{ name: "Indie" }],
          total_rating: 89,
          total_rating_count: 75,
          first_release_date: 1777377600
        }
      ]);
    }

    if (body.includes("platforms = (6,48,167,49,169)")) {
      return jsonResponse([
        {
          id: 77,
          name: "Halo Popular",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/halo-popular.jpg" },
          platforms: [
            { name: "Xbox Series X|S", slug: "series-x-s" },
            { name: "Nintendo Switch", slug: "switch" }
          ],
          total_rating: 86,
          total_rating_count: 90,
          hypes: 12,
          first_release_date: 1777377600
        }
      ]);
    }

    if (body.includes("where id = 123")) {
      return jsonResponse([
        {
          id: 123,
          name: "Halo Detail",
          cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/halo-detail.jpg" },
          platforms: [{ name: "Xbox" }],
          age_ratings: [
            {
              category: 2,
              rating: 4,
              rating_cover_url: "https://images.igdb.com/pegi-16.png"
            }
          ],
          external_games: [
            { category: 1, uid: "12345" }
          ],
          websites: [
            { category: 13, url: "https://store.steampowered.com/app/12345/Halo_Detail/" }
          ]
        }
      ]);
    }
  }

  if (urlString === "https://api.igdb.com/v4/genres") {
    return jsonResponse([
      { id: 31, name: "Action" },
      { id: 32, name: "Adventure" },
      { id: 33, name: "Indie" }
    ]);
  }

  if (urlString === "https://api.igdb.com/v4/themes") {
    return jsonResponse([
      { id: 1, name: "Action" }
    ]);
  }

  if (urlString === "https://api.igdb.com/v4/platforms") {
    return jsonResponse([
      { id: 6, name: "PC (Microsoft Windows)", slug: "win" },
      { id: 48, name: "PlayStation 4", slug: "ps4--1" },
      { id: 167, name: "PlayStation 5", slug: "ps5" },
      { id: 49, name: "Xbox One", slug: "xboxone" },
      { id: 169, name: "Xbox Series X|S", slug: "series-x-s" },
      { id: 130, name: "Nintendo Switch", slug: "switch" }
    ]);
  }

  if (urlString === "https://api.igdb.com/v4/release_dates") {
    return jsonResponse([
      {
        date: 1778371200,
        human: "May 10, 2026",
        platform: { name: "PC (Microsoft Windows)", slug: "win" },
        game: {
          id: 700,
          name: "Soon First",
          cover: { image_id: "soon-first" },
          summary: "First compact calendar item."
        }
      },
      {
        date: 1778457600,
        human: "May 11, 2026",
        platform: { name: "Xbox Series X|S", slug: "series-x-s" },
        game: {
          id: 701,
          name: "Soon Second",
          cover: { image_id: "soon-second" },
          summary: "Second compact calendar item."
        }
      }
    ]);
  }

  return jsonResponse({ error: "unexpected request" }, 500);
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}
