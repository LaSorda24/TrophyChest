import assert from "node:assert/strict";
import test from "node:test";
import {
  accountIdFromIdToken,
  getPlayStationAchievementsPayload,
  getPlayStationAuthPayload,
  getPlayStationTitleHistoryPayload,
  getPlayStationTrophyTitlesPayload,
  normalizeAchievements,
  normalizePlayedGames,
  normalizeProfile,
  normalizeTrophyTitles,
  parseIsoDurationMinutes
} from "../src/playstation.js";

test("parseIsoDurationMinutes handles PSN playtime durations", () => {
  assert.equal(parseIsoDurationMinutes("PT228H56M33S"), 13736);
  assert.equal(parseIsoDurationMinutes("P1DT2H3M4S"), 1563);
  assert.equal(parseIsoDurationMinutes("invalid"), 0);
});

test("getPlayStationAuthPayload rejects invalid NPSSO before calling PSN", async () => {
  let called = false;
  const psnClient = {
    exchangeNpssoForAccessCode: async () => {
      called = true;
    }
  };

  await assert.rejects(
    () => getPlayStationAuthPayload({ npsso: "short", psnClient }),
    /Invalid NPSSO/
  );
  assert.equal(called, false);
});

test("getPlayStationAuthPayload exchanges NPSSO and normalizes profile", async () => {
  const npsso = "a".repeat(64);
  const idToken = jwtWithPayload({ accountId: "123" });
  const payload = await getPlayStationAuthPayload({
    npsso,
    psnClient: {
      exchangeNpssoForAccessCode: async (value) => {
        assert.equal(value, npsso);
        return "v3.code";
      },
      exchangeAccessCodeForAuthTokens: async (code) => {
        assert.equal(code, "v3.code");
        return authTokens("refresh-1", idToken);
      },
      getProfileFromAccountId: async (_authorization, accountId) => {
        assert.equal(accountId, "123");
        return {
        profile: {
          accountId: "123",
          onlineId: "PlayerOne",
          avatars: [{ size: "l", url: "https://avatar.test/player.png" }]
        }
        };
      }
    }
  });

  assert.equal(payload.linkedAccount.onlineId, "PlayerOne");
  assert.equal(payload.auth.refreshToken, "refresh-1");
});

test("accountIdFromIdToken extracts account id from a JWT payload", () => {
  assert.equal(accountIdFromIdToken(jwtWithPayload({ accountId: "987654" })), "987654");
  assert.equal(accountIdFromIdToken(jwtWithPayload({ sub: "sub-account" })), "sub-account");
  assert.equal(accountIdFromIdToken("not-a-jwt"), null);
});

test("normalizeProfile uses onlineId and linked fallback label", () => {
  const profile = normalizeProfile(
    {
      onlineId: "PSNPlayer",
      avatars: [{ size: "l", url: "https://avatar.test/large.png" }]
    },
    "123"
  );
  const fallback = normalizeProfile(null, "456");

  assert.equal(profile.accountId, "123");
  assert.equal(profile.displayName, "PSNPlayer");
  assert.equal(profile.avatarUrl, "https://avatar.test/large.png");
  assert.equal(fallback.displayName, "Cuenta PlayStation vinculada");
});

test("normalizePlayedGames maps images, playtime and recency", () => {
  const games = normalizePlayedGames([
    {
      titleId: "PPSA00002_00",
      name: "Later Game",
      localizedName: "Later Game ES",
      imageUrl: "https://image.test/later-icon.jpg",
      category: "ps5_native_game",
      playDuration: "PT2H30M",
      playCount: 7,
      lastPlayedDateTime: "2026-04-20T10:00:00Z",
      media: { screenshotUrl: "https://image.test/later-hero.jpg" },
      concept: { titleIds: ["PPSA00002_00"], media: { images: [] } }
    },
    {
      titleId: "CUSA00001_00",
      name: "Earlier Game",
      localizedName: "",
      imageUrl: "",
      category: "ps4_game",
      playDuration: "PT45M",
      lastPlayedDateTime: "2026-04-01T10:00:00Z",
      concept: {
        titleIds: ["CUSA00001_00"],
        media: {
          images: [{ type: "FOUR_BY_THREE_BANNER", url: "https://image.test/banner.jpg" }]
        }
      }
    }
  ]);

  assert.equal(games[0].titleId, "PPSA00002_00");
  assert.equal(games[0].playtimeMinutes, 150);
  assert.equal(games[0].heroImageUrl, "https://image.test/later-hero.jpg");
  assert.deepEqual(games[1].supportedPlatforms, ["PlayStation 4"]);
  assert.equal(games[1].imageUrl, "https://image.test/banner.jpg");
});

test("normalizeTrophyTitles maps earned and defined trophy counts", () => {
  const titles = normalizeTrophyTitles([
    {
      npServiceName: "trophy2",
      npCommunicationId: "NPWR20188_00",
      trophySetVersion: "01.00",
      trophyTitleName: "Astro",
      trophyTitleIconUrl: "https://image.test/astro.png",
      trophyTitlePlatform: "PS5",
      hasTrophyGroups: false,
      definedTrophies: { bronze: 10, silver: 5, gold: 2, platinum: 1 },
      earnedTrophies: { bronze: 5, silver: 2, gold: 1, platinum: 0 },
      progress: 44,
      hiddenFlag: false,
      lastUpdatedDateTime: "2026-04-22T10:00:00Z"
    }
  ]);

  assert.equal(titles[0].npCommunicationId, "NPWR20188_00");
  assert.equal(titles[0].summary.total, 18);
  assert.equal(titles[0].summary.unlocked, 8);
  assert.equal(titles[0].npServiceName, "trophy2");
});

test("normalizeAchievements combines metadata and earned status", () => {
  const achievements = normalizeAchievements(
    [
      {
        trophyId: 1,
        trophyName: "First Step",
        trophyDetail: "Start the game",
        trophyType: "bronze",
        trophyIconUrl: "https://image.test/trophy.png"
      }
    ],
    [
      {
        trophyId: 1,
        earned: true,
        earnedDateTime: "2026-04-23T12:00:00Z",
        trophyEarnedRate: "52.4",
        trophyRare: 3
      }
    ]
  );

  assert.equal(achievements[0].title, "First Step");
  assert.equal(achievements[0].unlocked, true);
  assert.equal(achievements[0].globalPercentage, 52.4);
  assert.equal(achievements[0].unlockTime, 1776945600);
});

test("protected PSN endpoints require refresh token", async () => {
  await assert.rejects(
    () => getPlayStationTitleHistoryPayload({ refreshToken: "", psnClient: {} }),
    /Missing PlayStation refresh token/
  );
});

test("getPlayStationTitleHistoryPayload refreshes token and returns normalized games", async () => {
  const payload = await getPlayStationTitleHistoryPayload({
    refreshToken: "refresh-old",
    psnClient: {
      exchangeRefreshTokenForAuthTokens: async (refreshToken) => {
        assert.equal(refreshToken, "refresh-old");
        return authTokens("refresh-new");
      },
      getUserPlayedGames: async () => ({
        totalItemCount: 1,
        titles: [
          {
            titleId: "PPSA00002_00",
            name: "Play Game",
            localizedName: "Play Game",
            imageUrl: "https://image.test/play.jpg",
            category: "ps5_native_game",
            playDuration: "PT1H",
            lastPlayedDateTime: "2026-04-20T10:00:00Z"
          }
        ]
      })
    }
  });

  assert.equal(payload.auth.refreshToken, "refresh-new");
  assert.equal(payload.titles[0].playtimeMinutes, 60);
});

test("getPlayStationTrophyTitlesPayload returns normalized trophy titles", async () => {
  const payload = await getPlayStationTrophyTitlesPayload({
    refreshToken: "refresh-old",
    psnClient: {
      exchangeRefreshTokenForAuthTokens: async () => authTokens("refresh-new"),
      getUserTitles: async () => ({
        totalItemCount: 1,
        trophyTitles: [
          {
            npServiceName: "trophy",
            npCommunicationId: "NPWR00001_00",
            trophyTitleName: "Trophy Game",
            trophyTitleIconUrl: "https://image.test/trophy-game.png",
            trophyTitlePlatform: "PS4",
            definedTrophies: { bronze: 1 },
            earnedTrophies: { bronze: 1 },
            progress: 100,
            lastUpdatedDateTime: "2026-04-20T10:00:00Z"
          }
        ]
      })
    }
  });

  assert.equal(payload.titles[0].summary.progress, 1);
  assert.equal(payload.titles[0].npServiceName, "trophy");
});

test("getPlayStationAchievementsPayload combines title and user trophies", async () => {
  const payload = await getPlayStationAchievementsPayload({
    refreshToken: "refresh-old",
    npCommunicationId: "NPWR00001_00",
    npServiceName: "trophy",
    psnClient: {
      exchangeRefreshTokenForAuthTokens: async () => authTokens("refresh-new"),
      getTitleTrophies: async () => ({
        trophySetVersion: "01.00",
        hasTrophyGroups: false,
        trophies: [{ trophyId: 1, trophyName: "Winner", trophyType: "gold" }]
      }),
      getUserTrophiesEarnedForTitle: async () => ({
        lastUpdatedDateTime: "2026-04-20T10:00:00Z",
        trophies: [{ trophyId: 1, earned: true, trophyEarnedRate: "8.8" }]
      })
    }
  });

  assert.equal(payload.summary.total, 1);
  assert.equal(payload.summary.unlocked, 1);
  assert.equal(payload.trophies[0].globalPercentage, 8.8);
});

function authTokens(refreshToken, idToken = "") {
  return {
    accessToken: "access",
    expiresIn: 3600,
    idToken,
    refreshToken,
    refreshTokenExpiresIn: 5184000,
    tokenType: "Bearer"
  };
}

function jwtWithPayload(payload) {
  return [
    base64Url({ alg: "none" }),
    base64Url(payload),
    "signature"
  ].join(".");
}

function base64Url(value) {
  return Buffer.from(JSON.stringify(value), "utf8")
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}
