# Pickxo 1.8.0 — Home Screen Fix

The original Home screen was a placeholder: it literally rendered `Nothing here yet`.

This version keeps the native Kotlin + Jetpack Compose architecture and adds a real TMDB-powered Home screen.

## Added
- Trending this week
- Popular Movies
- Popular TV Shows
- Now Playing
- Upcoming
- Recommended for you
  - Uses genres found in the user's existing watchlist when available
  - Falls back to trending content for a new/empty library
- Poster cards using the existing Pickxo poster component
- Movie cards open the existing movie detail screen
- TV cards open the existing search flow with the selected title pre-filled
- Retry state when Home data cannot be loaded

## Important
The source ZIP intentionally does not contain `local.properties` or a TMDB API key.

Before building, create `local.properties` in the project root and add:

TMDB_API_KEY=YOUR_TMDB_API_KEY

The existing project already required this key; this fix does not change that requirement.
