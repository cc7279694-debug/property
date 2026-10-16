# Historical synthetic-save timeout — bounded diagnosis

The original historical XML/logs/screenshots remain unchanged under refinement. One bounded diagnostic run `task7-save-diagnostic` executed the original flow and original10,000ms assertion with added observation calls only:1 PASS, exit0,222.404s. This is NOT a root-cause resolution: taking diagnostics changes timing and can affect an intermittent driver failure.

Observed immediately before/after pointer click: callback0→1; resumed ComponentActivity has window focus; main thread answered a main-thread probe and was RUNNABLE; Compose root reports IME not visible/inset0 (input-method dumpsys has an inconsistent mInputShown=true, so no real visible keyboard claim). The synthetic callback is synchronous, no repository or DB. No evidence of a blocked business save in this sample. Original prior Digital Wellbeing ANR remains separate historical evidence.

A different geometric defect was measured: full Save bounds[42,1767,1038,1920], decor height1920, navigation inset63, safe boundary1857. A new independent boundary test actually failed on this exact overlap in task7-red. The minimal test-host safeDrawing padding aligns that host with the production Scaffold, without changing SyntheticFormHost, save/back/error/raw assertions or the10s wait. This fixes the measured unsafe target; it does not prove why the previous click sometimes timed out. Original callback timeout status remains INTERMITTENT / ROOT_CAUSE_UNKNOWN pending future evidence. Real native IME evidence is supplied separately in Task8.

Raw full window/input dumps remain ignored locally; the selected diagnostic lines here include only the test host and synthetic input. No private ledger or screenshots are copied by these diagnostics.
