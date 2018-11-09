import Action from '../models/Action';
export const testAction : Action = {
      "StartTime": "2018-08-17T11:36:16.003+01:00",
      "Name": "8 eni1 send MassQuote",
      "Description": "Send one-sided quote",
      "InputParameters": {
          "Name": "MassQuote",
          "SubParameters": [
              {
                "Name": "QuoteResponseLevel",
                "Value": "2"
              },
              {
                "Name": "QuotePublishMode",
                "Value": "2"
              },
              {
                "Name": "QuoteID",
                "Value": "1534502176319"
              },
              {
                "Name": "TransactTime",
                "Value": "2018-08-17T10:36:16.003000000"
              },
              {
                "Name": "NoQuoteSets",
                "SubParameters": [{
                    "Name": "0",
                    "SubParameters": [
                        {
                          "Name": "QuoteSetID",
                          "Value": "153450217631901"
                        },
                        {
                          "Name": "NoQuoteEntries",
                          "SubParameters" : [
                              {
                                "Name": "0",
                                "SubParameters": [
                                    {
                                      "Name": "BidNotionalAmount",
                                      "Value": "200"
                                    },
                                    {
                                      "Name": "QuoteEntryID",
                                      "Value": "153450217631901:1"
                                    },
                                    {
                                      "Name": "BidPx",
                                      "Value": "1.45"
                                    },
                                    {
                                      "Name": "Currency",
                                      "Value": "EUR"
                                    },
                                    {
                                      "Name": "NotionalCurrency",
                                      "Value": "EUR"
                                    },
                                    {
                                      "Name": "SecurityID",
                                      "Value": "120000011"
                                    },
                                    {
                                      "Name": "SecurityIDSource",
                                      "Value": "8"
                                    },
                                    {
                                      "Name": "BidSize",
                                      "Value": "1000"
                                    }
                                  ]
                              },
                              {
                                "Name": "1",
                                "SubParameters": [
                                    {
                                      "Name": "OfferNotionalAmount",
                                      "Value": "200"
                                    },
                                    {
                                      "Name": "OfferPx",
                                      "Value": "1.55"
                                    },
                                    {
                                      "Name": "OfferSize",
                                      "Value": "2000"
                                    },
                                    {
                                      "Name": "QuoteEntryID",
                                      "Value": "153450217631901:2"
                                    },
                                    {
                                      "Name": "Currency",
                                      "Value": "EUR"
                                    },
                                    {
                                      "Name": "NotionalCurrency",
                                      "Value": "EUR"
                                    },
                                    {
                                      "Name": "SecurityID",
                                      "Value": "120000011"
                                    },
                                    {
                                      "Name": "SecurityIDSource",
                                      "Value": "8"
                                    }
                                  ]
                              }
                            ]
                        }
                      ]
                }]
              }
            ]
      },
      "Status": {
          "Status": "PASSED"
      },
      "FinishTime": "2018-08-17T11:36:16.005+01:00",
      "ComparsionParameters": [
          {
              "Name": "testFlat",
              "Expected": "4",
              "Actual": "4",
              "Result": "PASSED"
          },
          {
              "Name": "testFlat2",
              "Expected": "null",
              "Actual": "3",
              "Result": "NA"
          },
          {
              "Name": "testContained",
              "SubParameters": [
                {
                    "Name": "testFlat",
                    "Expected": "4",
                    "Actual": "4",
                    "Result": "PASSED"
                },
                {
                    "Name": "testFlat2",
                    "Expected": "null",
                    "Actual": "3",
                    "Result": "NA"
                },
                {
                    "Name": "testSub",
                    "SubParameters": [
                        {
                            "Name": "someResult",
                            "Expected": "blabla",
                            "Actual": "blablabla",
                            "Result": "FAILED"
                        }
                    ]
                }
              ]
          }
      ]
    };