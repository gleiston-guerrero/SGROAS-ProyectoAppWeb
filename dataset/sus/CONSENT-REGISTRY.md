# SUS Participant Consent Registry

> **Corrected 2026-09-17 with real, verified evidence, and scope reduced to
> the official study (n=10).** The previous version of this file declared a
> single signing date (2026-08-15) for 15 participants that contradicted the
> repository's own git history (the P01-P10 evaluation results were already
> committed on 2026-07-30, before that claimed date). Signed, in-person paper
> consent forms for 15 people were subsequently located and verified — all
> 15 are real and their consent is genuine. However, only P01-P10 have
> verifiable documentary evidence that they actually answered the SUS
> questionnaire on the declared date; P11-P15's questionnaire answers have
> no such evidence (their values were hand-written into
> `scripts/update-sus.py` on 2026-09-06, ~6 weeks after the claimed
> evaluation date). P11-P15 are therefore **excluded from the official SUS
> study** below; their real, verified consent is documented separately in
> `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md` so as not to erase the fact
> that they exist and did consent. See
> `docs/etica/consentimientos/CONSENT-STATUS.md` for the full reconciliation
> and verification history.

Each participant signed an individual paper copy of the consent form
(`dataset/sus/CONSENT-FORM.md`) in person, before the evaluation session.
The signed originals are **not uploaded to this repository** (they contain
handwritten signatures, i.e. personally identifying information); each is
represented here only by its SHA-256 hash, which anyone holding the original
document can use to verify it matches this registry without exposing the
document itself.

| Code | Age | Gender | Device | Consent | Signed on  | Evaluated on | Medium              | SHA-256 of signed PDF |
|------|-----|--------|--------|---------|------------|--------------|----------------------|------------------------|
| P01  | 23  | Male   | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `a3034963a789df06cff212b43d2ef6e5c9b96ddc3cdf335e8b34b83e5c05739a` |
| P02  | 21  | Female | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `8381f4e8f0fe16dd05ddc668d200d5deefc5d1be5db66800ebdfe9c69cdec5f8` |
| P03  | 25  | Male   | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `33f4417909ee4fce3f2d13063445f10cbf1a5e70201ec41aa0f9904abcb726fb` |
| P04  | 23  | Female | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `b3d0a209390f4e43abf79fac827110b277aaf0de08ee8fdffc37e6290496cad3` |
| P05  | 20  | Female | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `8aeba8200d4b814820d4f3487f561c1820665bb769610ee118252cad5787c2f0` |
| P06  | 22  | Male   | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `a00523abf50a51937ebcf9b6b89d1e5014cf645172ca285b6e3ecc8214959b10` |
| P07  | 20  | Female | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `f36ed2050e67272494e7d3d6f96bd413d8cf3cf8502e9cd2f3203d7bcdb558fd` |
| P08  | 20  | Female | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `11891dd8e8a4fd382da0a82ff0cb7db2364a2eee8fc4a6d30754b79758645179` |
| P09  | 21  | Male   | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `db055bc4c2429933d98d6a6344c35c3e412d33948dcd9e3671a62af3d682e593` |
| P10  | 19  | Male   | Desktop| Yes     | 2026-07-24 | 2026-07-30   | In-person, paper, scanned | `b0b864109c3665c9430a8b6bab5c7d1cbd22d39ac3759843a56dfdd826d283df` |

All 10 hashes were computed independently with two different tools
(`sha256sum` and Python's `hashlib.sha256`) against the same 10 files on
2026-09-17, both producing identical results; no two hashes collide (10
distinct files, 10 distinct hashes). Every signed form was also checked to
contain a legible signing date matching the table above, extracted with
`pdftotext -layout`.

All 10 participants provided written informed consent **before** their
respective evaluation session (6 days before).

## P11-P15: consent is real, survey data is not used

5 more people (P11-P15) also signed real, verified consent forms (signed on
2026-07-26). They are **not part of the official SUS study above** because
their SUS questionnaire answers have no verifiable documentary evidence of
the date they were collected. Their consent is not in question — only their
survey data's traceability is. See `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md`
for the full explanation and their preserved consent hashes.
