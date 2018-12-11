# zuora-price-riser

Script to rise the price of Guardian Weekly subscriptions on Annual and Quarterly billing periods.

## How to run

1. The script requires [Zuora Oauth Authentication](https://knowledgecenter.zuora.com/CF_Users_and_Administrators/A_Administrator_Settings/Manage_Users#Create_an_OAuth_Client_for_a_User) credentials:

    ```
    export ZUORA_STAGE=DEV
    export ZUORA_CLIENT_SECRET=**********
    export ZUORA_CLIENT_ID=**********
    ```
2. Drop the import file at project root an run `Main.scala`:

    ```
    sbt
    run file.csv
    ```
3. Tail the logs: `tail -f logs/application.log`


