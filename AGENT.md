# Agent Instructions

You are building a Kotlin Android time tracking app. The Goal is to communicate with a external Server via REST api.

## Architecture
- Use MVVM
- Use Retrofit for API calls
- Use Coroutines for async operations

## Rules
- Always create or modify real files in the project directory
- Never use absolute paths
- Prefer incremental changes
- After each feature, ensure app builds
- After succesfully implementing a feature and succesfull build commit changes

## API Integration
- Use Retrofit interfaces
- Centralize API client in one file

## API Reference

The OpenAPI specification is located at:

/docs/openapi.json

When implementing API calls:
- Always read this file first
- Do not guess endpoints or payloads
- Extract request/response schemas from the spec
- Generate strongly typed models from it

## Workflow
1. Plan before coding
2. Implement feature in small steps
3. Verify structure consistency

## Build & Test Loop

After every code change:

1. Run the project build command:
   - For Android: `./gradlew build`

2. If there are compilation errors:
   - Read full error output carefully
   - Identify root cause (not symptoms)
   - Apply minimal fix only
   - Do not refactor unrelated code

3. Repeat build until successful

4. Never assume code is correct without running build