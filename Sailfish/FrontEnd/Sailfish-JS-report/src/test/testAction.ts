import Action from '../models/Action';
export const testAction : Action = {
      "uuid": "6ab515a9-5f6a-4773-b7df-b2e80ec9108e",
      "startTime": "2018-08-17T11:36:16.003+01:00",
      "name": "8 eni1 send MassQuote",
      "relatedMessages": ["95259138-c905-4863-8189-d93bc8808660"],
      "description": "Send one-sided quote",
          "parameters": [
              {
                "name": "QuoteResponseLevel",
                "value": "2"
              },
              {
                "name": "QuotePublishMode",
                "value": "2"
              },
              {
                "name": "QuoteID",
                "value": "1534502176319"
              },
              {
                "name": "TransactTime",
                "value": "2018-08-17T10:36:16.003000000"
              },
              {
                "name": "NoQuoteSets",
                "parameters": [{
                    "name": "0",
                    "parameters": [
                        {
                          "name": "QuoteSetID",
                          "value": "153450217631901"
                        },
                        {
                          "name": "NoQuoteEntries",
                          "parameters" : [
                              {
                                "name": "0",
                                "parameters": [
                                    {
                                      "name": "BidNotionalAmount",
                                      "value": "200"
                                    },
                                    {
                                      "name": "QuoteEntryID",
                                      "value": "153450217631901:1"
                                    },
                                    {
                                      "name": "BidPx",
                                      "value": "1.45"
                                    },
                                    {
                                      "name": "Currency",
                                      "value": "EUR"
                                    },
                                    {
                                      "name": "NotionalCurrency",
                                      "value": "EUR"
                                    },
                                    {
                                      "name": "SecurityID",
                                      "value": "120000011"
                                    },
                                    {
                                      "name": "SecurityIDSource",
                                      "value": "8"
                                    },
                                    {
                                      "name": "BidSize",
                                      "value": "1000"
                                    }
                                  ]
                              },
                              {
                                "name": "1",
                                "parameters": [
                                    {
                                      "name": "OfferNotionalAmount",
                                      "value": "200"
                                    },
                                    {
                                      "name": "OfferPx",
                                      "value": "1.55"
                                    },
                                    {
                                      "name": "OfferSize",
                                      "value": "2000"
                                    },
                                    {
                                      "name": "QuoteEntryID",
                                      "value": "153450217631901:2"
                                    },
                                    {
                                      "name": "Currency",
                                      "value": "EUR"
                                    },
                                    {
                                      "name": "NotionalCurrency",
                                      "value": "EUR"
                                    },
                                    {
                                      "name": "SecurityID",
                                      "value": "120000011"
                                    },
                                    {
                                      "name": "SecurityIDSource",
                                      "value": "8"
                                    }
                                  ]
                              }
                            ]
                        }
                      ]
                }]
              }
            ]
      ,
      "status": "PASSED",
      "finishTime": "2018-08-17T11:36:16.005+01:00",
      "verifications": [
          {
              "name": "testFlat",
              "expected": "4",
              "actual": "4",
              "result": "PASSED"
          },
          {
              "name": "testFlat2",
              "expected": "null",
              "actual": "3",
              "result": "NA"
          },
          {
              "name": "testContained",
              "parameters": [
                {
                    "name": "testFlat",
                    "expected": "4",
                    "actual": "4",
                    "result": "PASSED"
                },
                {
                    "name": "testFlat2",
                    "expected": "null",
                    "actual": "3",
                    "result": "NA"
                },
                {
                    "name": "testSub",
                    "parameters": [
                        {
                            "name": "someresult",
                            "expected": "blabla",
                            "actual": "blablabla",
                            "result": "FAILED"
                        }
                    ]
                }
              ]
          }
      ]
    };