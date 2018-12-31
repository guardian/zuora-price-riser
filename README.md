# zuora-price-riser

Script to rise the price of Guardian Weekly subscriptions on Annual and Quarterly billing periods.

## How to run

Make sure to run the script after the bill run completes for the day. Currently bill run happens at 6 am and lasts around 3 hrs.

1. The script requires [Zuora Oauth Authentication](https://knowledgecenter.zuora.com/CF_Users_and_Administrators/A_Administrator_Settings/Manage_Users#Create_an_OAuth_Client_for_a_User) credentials:

    ```
    export ZUORA_STAGE=DEV
    export ZUORA_CLIENT_SECRET=**********
    export ZUORA_CLIENT_ID=**********
    ```
2. Drop the import file at project root and run `Main.scala` with:

    ```
    sbt "run file.csv"
    ```
    
3. Choose between dry run (only checks pre-conditions and outputs stats) and main run (write to Zuora):

    ```
    Multiple main classes detected, select one to run:

     [1] com.gu.DryRunner
     [2] com.gu.Main
    [info] Packaging /Users/mgalic/sandbox/zuora-price-riser/target/scala-2.12/zuora-price-riser_2.12-0.1.0-SNAPSHOT.jar ...

    Enter number: [info] Done packaging.
    1

    [info] Running com.gu.DryRunner subs.csv
    2018-12-19 19:00:43,518 [INFO] - Start dry run processing subs.csv...
    2018-12-19 19:00:45,993 [INFO] - Dry run completed for subs.csv
    2018-12-19 19:00:45,993 [INFO] - --------------------------------------------------------------
    2018-12-19 19:00:45,993 [INFO] - Results (count):
    2018-12-19 19:00:45,993 [INFO] - --------------------------------------------------------------
    2018-12-19 19:00:45,994 [INFO] - Unsatisfied PriceIsWithinReasonableBounds: 2
    2018-12-19 19:00:45,994 [INFO] - Unsatisfied BillingPeriodIsQuarterlyOrAnnually: 1
    2018-12-19 19:00:45,994 [INFO] - Unsatisfied SubscriptionIsAutoRenewable: 2
    2018-12-19 19:00:45,994 [INFO] - Price rise already applied: 1
    2018-12-19 19:00:45,994 [INFO] - Term extension applied: 0
    ```
 
4. Tail the logs: `tail -f logs/application.log`

## Errors handling

Script is designed to stop on first error it encounters. After the error:
  1. examine the logs to determine the cause,
  2. fix the cause
  3. re-run the script (the script should be idempotent)
  
Example error log:

```
2018-12-11 16:22:38,316 [INFO] - Start processing subs.csv...
2018-12-11 16:22:41,618 [ERROR] - A-S00048031 failed because of unsatisfied pre-conditions: List(ImportHasCorrectCurrentPrice, TargetPriceRiseIsNotMoreThanTheCap)
2018-12-11 16:22:41,618 [ERROR] - Aborted due to error. Please examine the logs, fix the error, and re-run the script.
```

Note the difference between `SkipReason` and `CheckPriceRisePreCondition`:
* `SkipReason` - valid business reason (for example, one off, cancelled)
* `unsatisfied precondtions` - error in the input file or Zuora state
