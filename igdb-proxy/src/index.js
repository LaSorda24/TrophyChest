import {
  getCategoryGamesPayload,
  getExploreGamesPayload,
  getGameDetailsPayload,
  getSearchGamesPayload
} from "./igdbGames.js";
import {
  getPlayStationAchievementsPayload,
  getPlayStationAuthPayload,
  getPlayStationStatusPayload,
  getPlayStationTitleHistoryPayload,
  getPlayStationTrophyTitlesPayload
} from "./playstation.js";
import { getReleaseCalendarPayload } from "./releaseCalendar.js";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization"
};

// APUNTE: ESTE WORKER HACE DE PUENTE ENTRE LA APP Y SERVICIOS QUE NO QUIERO LLAMAR DIRECTO DESDE ANDROID.
export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    const url = new URL(request.url);
    try {
      if (url.pathname === "/psn/status") {
        if (request.method !== "GET") return methodNotAllowed();
        return jsonResponse(getPlayStationStatusPayload(), 200, {
          "Cache-Control": "no-store"
        });
      }

      if (url.pathname === "/psn/auth/npsso") {
        if (request.method !== "POST") return methodNotAllowed();
        const payload = await getPlayStationAuthPayload(await readJsonBody(request));
        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      if (url.pathname === "/psn/title-history") {
        if (request.method !== "POST") return methodNotAllowed();
        const payload = await getPlayStationTitleHistoryPayload(await readJsonBody(request));
        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      if (url.pathname === "/psn/trophy-titles") {
        if (request.method !== "POST") return methodNotAllowed();
        const payload = await getPlayStationTrophyTitlesPayload(await readJsonBody(request));
        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      const psnAchievementsMatch = url.pathname.match(/^\/psn\/achievements\/([^/]+)$/);
      if (psnAchievementsMatch) {
        if (request.method !== "POST") return methodNotAllowed();
        const body = await readJsonBody(request);
        const payload = await getPlayStationAchievementsPayload({
          ...body,
          npCommunicationId: psnAchievementsMatch[1]
        });
        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      if (request.method !== "GET") {
        return methodNotAllowed();
      }

      if (url.pathname === "/release-calendar") {
        const payload = await getReleaseCalendarPayload({
          from: url.searchParams.get("from"),
          days: url.searchParams.get("days"),
          limit: url.searchParams.get("limit"),
          clientId: env.TWITCH_CLIENT_ID,
          clientSecret: env.TWITCH_CLIENT_SECRET,
          fetchFn: fetch,
          now: new Date()
        });

        return jsonResponse(payload, 200, {
          "Cache-Control": "public, max-age=1800"
        });
      }

      if (url.pathname === "/search-games") {
        const payload = await getSearchGamesPayload({
          query: url.searchParams.get("q"),
          clientId: env.TWITCH_CLIENT_ID,
          clientSecret: env.TWITCH_CLIENT_SECRET,
          fetchFn: fetch,
          now: new Date()
        });

        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      if (url.pathname === "/explore-games") {
        const payload = await getExploreGamesPayload({
          clientId: env.TWITCH_CLIENT_ID,
          clientSecret: env.TWITCH_CLIENT_SECRET,
          fetchFn: fetch,
          now: new Date(),
          genres: url.searchParams.get("genres")
        });

        return jsonResponse(payload, 200, {
          "Cache-Control": "public, max-age=1800"
        });
      }

      if (url.pathname === "/category-games") {
        const payload = await getCategoryGamesPayload({
          genre: url.searchParams.get("genre"),
          clientId: env.TWITCH_CLIENT_ID,
          clientSecret: env.TWITCH_CLIENT_SECRET,
          fetchFn: fetch,
          now: new Date()
        });

        return jsonResponse(payload, 200, {
          "Cache-Control": "public, max-age=1800"
        });
      }

      const gameDetailsMatch = url.pathname.match(/^\/games\/(\d+)$/);
      if (gameDetailsMatch) {
        const payload = await getGameDetailsPayload({
          gameId: gameDetailsMatch[1],
          clientId: env.TWITCH_CLIENT_ID,
          clientSecret: env.TWITCH_CLIENT_SECRET,
          fetchFn: fetch,
          now: new Date()
        });

        return jsonResponse(payload, 200, {
          "Cache-Control": "no-store"
        });
      }

      return jsonResponse({ error: "Not found" }, 404);
    } catch (error) {
      return jsonResponse(
        { error: error.publicMessage || "No se pudo cargar el calendario de estrenos." },
        error.statusCode || 500
      );
    }
  }
};

async function readJsonBody(request) {
  try {
    return await request.json();
  } catch {
    const error = new Error("Invalid JSON body.");
    error.statusCode = 400;
    error.publicMessage = "Cuerpo JSON no valido.";
    throw error;
  }
}

function methodNotAllowed() {
  return jsonResponse({ error: "Method not allowed" }, 405);
}

function jsonResponse(body, status, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      ...headers,
      "Content-Type": "application/json; charset=utf-8"
    }
  });
}
