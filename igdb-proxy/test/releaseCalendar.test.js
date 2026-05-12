import assert from "node:assert/strict";
import test from "node:test";
import {
  applyReleaseLimit,
  buildReleaseDatesQuery,
  coverImageUrl,
  createTokenProvider,
  normalizeReleaseCalendar
} from "../src/releaseCalendar.js";

test("createTokenProvider reuses token before expiry", async () => {
  let calls = 0;
  const getToken = createTokenProvider(async () => {
    calls += 1;
    return {
      ok: true,
      json: async () => ({ access_token: "token-1", expires_in: 3600 })
    };
  }, () => 1000);

  assert.equal(await getToken("client", "secret"), "token-1");
  assert.equal(await getToken("client", "secret"), "token-1");
  assert.equal(calls, 1);
});

test("buildReleaseDatesQuery contains platform and date filters", () => {
  const query = buildReleaseDatesQuery({
    fromEpoch: 1776384000,
    toEpoch: 1784160000,
    platformIds: [6, 48, 167]
  });

  assert.match(query, /fields date,human,date_format/);
  assert.match(query, /date >= 1776384000/);
  assert.match(query, /date < 1784160000/);
  assert.match(query, /date_format = 0/);
  assert.match(query, /release_region = \(1,8\)/);
  assert.match(query, /platform = \(6,48,167\)/);
  assert.match(query, /game.category = null/);
});

test("coverImageUrl maps image_id to IGDB cover URL", () => {
  assert.equal(
    coverImageUrl("abc123"),
    "https://images.igdb.com/igdb/image/upload/t_cover_big/abc123.jpg"
  );
  assert.equal(coverImageUrl(null), null);
});

test("normalizeReleaseCalendar deduplicates game by release day and groups platforms", () => {
  const releases = normalizeReleaseCalendar([
    {
      date: 1778371200,
      human: "May 10, 2026",
      platform: { name: "PC (Microsoft Windows)", slug: "win" },
      game: {
        id: 7,
        name: "Shared Launch",
        category: 0,
        cover: { image_id: "cover7" },
        summary: "A future release."
      }
    },
    {
      date: 1778371200,
      human: "May 10, 2026",
      platform: { name: "PlayStation 5", slug: "ps5" },
      game: {
        id: 7,
        name: "Shared Launch",
        category: 0,
        cover: { image_id: "cover7" },
        summary: "A future release."
      }
    }
  ]);

  assert.equal(releases.length, 1);
  assert.deepEqual(releases[0].platformFamilies.sort(), ["PC", "PLAYSTATION"]);
  assert.deepEqual(releases[0].platformNames, ["PC (Microsoft Windows)", "PlayStation 5"]);
});

test("applyReleaseLimit trims normalized releases after sorting", () => {
  const releases = [
    { igdbGameId: 1, releaseDate: "2026-05-10", name: "First" },
    { igdbGameId: 2, releaseDate: "2026-05-11", name: "Second" },
    { igdbGameId: 3, releaseDate: "2026-05-12", name: "Third" }
  ];

  assert.deepEqual(applyReleaseLimit(releases, 2), releases.slice(0, 2));
  assert.deepEqual(applyReleaseLimit(releases, null), releases);
});
