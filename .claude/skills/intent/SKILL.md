---
name: intent
description: Capture a problem or idea as a reviewed, committed `intent.md` — the first artifact of the AI-native SDLC (Plan stage). Use whenever the user runs /intent, says they want to "write up an idea", "capture intent", "scope a feature/bug/change", "brainstorm before building", "write a product brief", or describes a problem in plain language before any spec, plan, or code exists. Also use when a monitoring finding or incident needs to re-enter the pipeline as new work. Do this BEFORE spec.md or plan.md, and do not start designing or implementing from here. Skip it when the user already has an accepted intent or spec, or only wants a quick answer about a change rather than a written artifact.
---

# /intent — capture intent once, in the originator's words

Based on the Plan stage of the [AI-native SDLC playbook](https://claude.com/resources/articles/the-ai-native-sdlc-playbook).

Code is no longer the bottleneck; misunderstood requirements are. The cheapest place to fix a misunderstanding is before a spec or plan exists. So this skill does one job: turn a loose problem statement into a short, correct, committed `intent.md` that every later stage (`spec.md` → `plan.md` → build → test → deploy) reads. It stops there. Resist the urge to design solutions or write code — that is the next stage's job, and doing it here bakes in assumptions the product owner hasn't reviewed.

## Workflow

1. **Take the problem in plain language.** Use the user's arguments or message. If nothing was given, ask for a few sentences: what can't be done today, who is affected, what "better" looks like, and what is explicitly out of scope.

2. **Ground yourself quickly.** Check the repo for an existing `intent/` folder (to find the next number) and relevant code or docs. Existing intents show naming and tone; the code shows which systems are actually affected. Keep this brief — it is to ask better questions, not to design.

3. **Brainstorm until concrete.** Ask focused questions about scope, users, constraints, and how success will be recognized. Ask only what you can't infer, in small batches (2–4 questions), and propose a default when you have a sensible one so the user can just confirm. Stop when the Problem and Proposed outcome could be read by a stranger and understood the same way. Don't interrogate for completeness; anything still unknown belongs in Open questions. If the user can't or won't answer, don't keep asking: after one round, record the gaps in Open questions and move on to the draft, flagging that it rests on those gaps.

4. **Draft `intent.md`** using this template:

   ```markdown
   # Intent: <title>
   Author: <name>

   ## Problem
   What can't be done today, who is affected, why it matters. Plain language, the originator's own words where possible.

   ## Proposed outcome
   What better looks like, stated as observable results (what a user can do, what a metric does), not implementation. Include what is out of scope. Where possible phrase outcomes so they can later be verified (e.g. "an owner can add a pet with no birth date and see a clear error").

   ## Affected users and systems
   Who and what is touched: user groups, services, modules, data, integrations.

   ## Constraints
   Security, compliance, brand, performance, deadlines, compatibility, budget, anything non-negotiable.

   ## Open questions
   Unknowns and decisions someone must resolve, each with a suggested owner if known.
   ```

   For Author, use `git config user.name`; if it's unset, or the person you're talking to is clearly someone else, ask rather than guess. Keep the whole file to about a page: it is read by people and by agents, and length dilutes it. Describe *what* and *why*; leave *how* to `spec.md` and `plan.md`.

5. **Review with the user before committing.** Show the draft (or its path) and ask them to correct misunderstandings. The product owner's correction is the point of the stage — an unreviewed intent is just a guess. Revise until they accept.

6. **Save and commit — with permission.** Write to the shared intent home as `intent/<NNN>-<kebab-name>/intent.md`, e.g. `intent/001-example-feature/intent.md`. Each intent gets its own numbered folder so the later `spec.md` and `plan.md` can sit beside it. Pick `NNN` by listing `intent/`, considering only folders named `NNN-*`, taking the highest prefix, and adding one (start at `001`; create `intent/` if missing). Numbers can collide when two branches add intents from the same base, so check again for a clash before committing and renumber yours if needed. Derive the kebab-case name from the title, short and lowercase. If the repo already uses a different layout, mention it and ask before diverging. Writing the file is fine. Only run `git commit` if the user has asked for it or agrees when you offer; stage only the intent file.

7. **Hand off.** Tell the user the file path and that the next step is turning the accepted intent into `spec.md` in the same folder (requirements and design constrained by org skills for security, brand, compliance, UX), then `plan.md` in plan mode. Don't start those unprompted.

## Intents that come from monitoring or incidents

When the input is a finding (a control-band breach, a failed security scan, an incident), treat it the same way: the finding is the Problem, with evidence (metric, time window, key log lines) quoted rather than paraphrased. Keep it short: summarize long output and link to the full source instead of pasting it, and redact secrets, tokens, hostnames, and personal data, since the file gets committed. This closes the loop — Maintain feeds new intent back into Plan through the same review gate.

## Quality bar

- A reader with no context can state the problem and the desired outcome after one read.
- Outcomes are observable and, where feasible, checkable — they become the Proof section of `plan.md` later.
- Nothing in it prescribes classes, libraries, or file edits.
- Unresolved items are in Open questions rather than silently decided.
- The user's wording is preserved where it carries meaning; don't launder it into generic product-speak.
