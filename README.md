# ratpack-bank-application
Simple demo application of a bank based on Ratpack

# Assumptions
- Java 8
- use ratpack to experiment with new framework and write application in non-blocking style
- data held in memory (no DB)
- two accounts (REV1 and REV2) with 1000 PLN each get created at start

# Endpoints
- GET */accounts/{accountName}* - basic account details (balance, blocked money)
- GET */accounts/{accountName}/transfers* - awaiting transfers for account
- PUT */accounts/{accountName}/transfers/{transferUUID}* - issue a new money transfer
- PUT *admin/suspension?suspend={true|false}* - for testing purpose to simulate delays in Event handling

# Tests
- *BankAccountTest* - unit Test of BankAccountEntity as it contains main logic and aggregates other objects
- *BankIntegrationTest* - Integration Test that uses REST API

# Possible improvements:
- use Concordion for Integration Tests
- define DTOs using Immutables or just in code
- add links in DTOs to other endpoints (HATEOS)
