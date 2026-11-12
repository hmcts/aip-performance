# AIP-Performance

Description of AIP Project

This release of IAC is to delivery the additional capability for Appellants-in Person (private individuals) to submit an appeal, 
upload supporting documentation and track appeal status. 
The Appellants will access the system using IAC appellants UI. The workflow for Legal reps, Home Office, 
Case Workers to manage and progress cases, Judiciary to review the case remains unchanged.
1. Appellant uses Submit Appeal web application to submit and appeal (https://immigration-appeal.perftest.platform.hmcts.net)
2. Manage appeal web application allows appellant to view case, provide further evidence, upload supporting documents.
3. IDAM is used to authenticate the user credentials
4. Application submitted by appellants via Submit appeals are stored in CCD
5. Documents uploaded through manage appeal are stored into Document store and also updates CCD with url of the stored document
   Notify is used to send notification of case creation and progression of the case
6. Data is validated with the Home Office at different stages through the application process 
7. A mock is available to avoid overloading the Home Office test environment 
8. The Home Office integration (and data) can be used occassionally and not for load testing
9. Toggle between using the mock or Home Office connectivity by changing `USE_MOCK` in the scenario file.


To run locally:
- Performance test against the perftest environment: `./gradlew gatlingRun`

Flags:
- Debug (single-user mode): `-Ddebug=on e.g. ./gradlew gatlingRun -Ddebug=on`
- Run against AAT: `Denv=aat e.g. ./gradlew gatlingRun -Denv=aat`
