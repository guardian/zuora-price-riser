# zuora-price-riser

Script to rise the price of Guardian Weekly subscriptions on Annual and Quarterly billing periods.

## How to run

1. The script requires [Zuora Oauth Authentication](https://knowledgecenter.zuora.com/CF_Users_and_Administrators/A_Administrator_Settings/Manage_Users#Create_an_OAuth_Client_for_a_User) credentials:

    ```
    export ZUORA_STAGE=DEV
    export ZUORA_CLIENT_SECRET=**********
    export ZUORA_CLIENT_ID=**********
    ```
2. Drop the import file at project root an run `Main.scala` with:

    ```
    sbt "run file.csv"
    ```
3. Tail the logs: `tail -f logs/application.log`

## Errors handling

Script is designed to stop on first error it encounters. After the error:
  1. examine the logs to determine the cause,
  2. fix the cause
  3. re-run the script (the script should be idempotent)
  
Example error log:

```
[info] Running com.gu.Main subs.csv
2018-12-11 16:22:38,316 [INFO] from com.gu.Main$ in sbt-bg-threads-1 - Start processing subs.csv...
2018-12-11 16:22:41,618 [ERROR] from com.gu.Abort$ in sbt-bg-threads-1 - A-S00048031 failed because of unsatisfied pre-conditions: List(ImportHasCorrectCurrentPrice, TargetPriceRiseIsNotMoreThanTheCap)
2018-12-11 16:22:41,618 [ERROR] from com.gu.Abort$ in sbt-bg-threads-1 - Aborted due to error. Please examine the logs, fix the error, and re-run the script.
```


