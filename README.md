# ratpack-bank-application
Simple demo application of a bank based on Ratpack

# Assumptions
- use ratpack to experiment with new framework and write application in non-blocking style
- data held in memory (no DB)

# Endpoints
- basic account details (balance, blocked money)
- issue a new money transfer
- get awaiting transfers for account

# Tests
- unit Test of BankAccountEntity as it contains main logic and aggregates other objects
- Integration Test that uses REST API

# Possible improvements:
- use Concordion for Integration Tests
- define DTOs using Immutables or just in code
- add links in DTOs to other endpoints (HATEOS)
