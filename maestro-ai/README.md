# maestro-ai

This project implements AI support for use in Maestro.

It's both a library and an executable demo app.

### Demo app

An API key is required. Set it with `MAESTRO_CLI_AI_KEY` env var. Examples:
- OpenAI: `export MAESTRO_CLI_AI_KEY=sk-...`
- Antrophic: `export MAESTRO_CLI_AI_KEY=sk-ant-api-...`

Build it:

```console
./gradlew :maestro-ai:installDist
```

then learn how to use it:

```console
./maestro-ai/build/install/maestro-ai-demo/bin/maestro-ai-demo --help
```

Finally, run it:
