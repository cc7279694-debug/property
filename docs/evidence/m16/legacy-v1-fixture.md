# M16 retained format-v1 synthetic fixture

`app/src/androidTest/assets/m16/legacy-v1.lithicbackup` is the immutable original produced before any M16 production backup-file changes. The unchanged M15 format-v1 writer/validator (baseline `661d2b5addc4eca377756f08edb84668ba700736`) generated and read back this small synthetic archive in the first RED batch. It was not produced by exporting v2 and removing new fields.

The generator source is [M16LegacyV1FixtureGenerationTest.kt](reproduction/M16LegacyV1FixtureGenerationTest.kt). It is retained for provenance only, outside `src/test`, and is not rerun against the new v2 exporter. The original generation/RED output is [backup-red-fixture-2.txt](logs/backup-red-fixture-2.txt).

- Archive length: 1803 bytes.
- Archive SHA-256: `d7dd7e8bf105721cfeeed687df977bf69921ca5c28e8e9d8d083beb60becb818`.
- Manifest: format 1 / minimum reader 1 / source Room schema 2; seven original table count keys.
- Data: two fixed fallback categories, one project, one initial cash event, one subscription, one real payment, one paused state and one registered PNG. Settings use `Asia/Shanghai`.
- Image is an original deterministic 1x1 RGBA pixel (43/71/89/255), PNG filter 0, JDK zlib stream and explicit CRC32 per chunk. No private media or ledger data.
- Fixture is never regenerated, replaced or edited. Android checks the full archive length/SHA before parsing; current export now produces v2 independently.

| Original entry | Bytes | SHA-256 |
| --- | ---: | --- |
| `manifest.json` | 739 | `7b737dc8bb426b57b1bacddc8ffaa7450a44f40ce810e8c8b8bb0801c4c58154` |
| `data.json` | 2006 | `18f5c6a8d0b30832465268f261dd0ce6579f659dec05fcd7683f6c63cccf0513` |
| `media/m16-legacy-original.png` | 70 | `2690870ae3da3a57595a415444bc765ce854e8c8ef688b38e18346a082ef02b9` |
| `settings.json` | 32 | `754f38d01cfe4caa6c26b89508f329e669e8e8a950b99190b09669807e1ce744` |
