# MobiWave CnC UI system and verification checklist

Scope: public landing page, authenticated web dashboard/admin workflows, and the native Android app. This document defines the shared visual baseline; it is not a claim that every screen or device has already passed QA.

## Brand and semantic colours

| Token | Value | Use |
|---|---|---|
| Brand teal | `#087F63` | Primary actions, links, selected navigation |
| Brand hover | `#066D55` | Hover/pressed primary action |
| Ink | `#17263A` | Primary text |
| Muted text | `#59677A` | Supporting copy; check contrast before using at small sizes |
| Border | `#DCE3EB` | Control and panel outlines |
| Canvas | `#F5F7FA` | Web application background |
| Surface | `#FFFFFF` | Panels and form controls |
| Success | `#087F63` on pale green `#E3F7EE` | Completed/verified state only when source confirms it |
| Warning | `#8A4B08` on pale amber `#FFF4D8` | Pending/manual review/attention |
| Error | `#B42318` on pale red `#FFEBEB` | Failed/rejected/unsafe action |
| Info | `#245A83` on pale blue `#EAF3FB` | Neutral information |

Status must always include text or another non-colour cue. “Invoice checked” must not be represented as “invoice verified” unless the configured trusted provider returned a recognised positive result.

## Typography and spacing

- Web: DM Sans for body copy and Manrope for headings; native Android uses the system font family.
- Android text styles are centralised in Material 3 `Typography`; preserve readable body sizes and allow wrapping instead of truncating financial values.
- Spacing rhythm: 4, 8, 12, 16, 24 px on web; 4/8/12/16/24 dp equivalents on Android.
- Controls: 8 px web control radius; 10 px web panel radius. Keep repeated actions visually consistent.
- Minimum intended web interactive target: 44 px where practical; Android Material controls should preserve their standard touch target sizes.

## Responsive and interaction rules

- Test viewport widths: 320, 375, 768, 1024 and wide desktop.
- On narrow web screens, the landing navigation wraps, the hero stacks, dashboard navigation wraps, forms become single-column, and wide data tables stay inside their own horizontal scroll region.
- Android's finance/review action strip stacks at widths below 380 dp.
- Keyboard users need a visible focus ring. Selectable enterprise request rows support Enter and Space as well as pointer selection.
- Reduced-motion preference should suppress decorative animation and smooth scrolling.
- Form errors and action feedback should remain visible and announced with appropriate alert/status semantics. Dialog focus, escape behaviour and screen-reader output still require device/browser testing.

## Required verification before declaring UI QA complete

- [ ] Build web app; run lint and automated tests if configured (currently `web/package.json` exposes build/dev/preview scripts only).
- [ ] Run Android unit tests and debug build using repository CI / supported Gradle environment.
- [ ] Inspect screenshots at 320, 375, 768, 1024 and desktop for landing, sign-in, dashboard, enterprise requests and major dialogs.
- [ ] Check horizontal overflow, table scrolling, sidebar navigation and touch targets.
- [ ] Keyboard-only pass for sign-in, navigation, table selection, forms and dialogs.
- [ ] Check visible focus, accessible names, validation errors, live announcements and dialog focus return.
- [ ] Verify light and dark theme contrast with an automated contrast tool and real screenshots. The Android theme defines light/dark schemes, but runtime theme switching and dark-mode parity must be confirmed.
- [ ] Verify reduced motion on web and Android.
- [ ] Regression-test authentication, role/permission boundaries, billing, invoice review and API actions; visual changes must not change these behaviours.
- [ ] Capture before/after screenshots and attach build/test evidence to the UI review.

## Current implementation notes

The UI hardening branch adds web design tokens and focus styles, improves small-screen landing/dashboard layout behaviour, makes enterprise-request table selection keyboard-operable, centralises Android typography, adjusts one light-theme container text colour, and stacks the Android finance/review action row on very narrow screens. These changes have not yet been certified by a complete browser matrix, screen-reader pass, physical-device test or end-to-end regression run.
