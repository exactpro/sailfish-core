import TestCase from "../models/TestCase";

export const testCase : TestCase = {
    "actions": [
      {
        "uuid": "6ab515a9-5f6a-4773-b7df-b2e80ec9108e",
        "name": "TestAction1",
        "description": "TestDescription1",
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
          ],
        "relatedMessages": ["95259138-c905-4863-8189-d93bc8808660"],
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
        ],
        "startTime": null,
        "status" : "FAILED"
      }
    ],
    "logs": [],
    "messages": [
      {
        "uuid": "95259138-c905-4863-8189-d93bc8808660",
        "fields": {"fieldname": "fieldvalue"},
        "rawContent": null,
        "type": null,
        "relatedActions": ["6ab515a9-5f6a-4773-b7df-b2e80ec9108e"]
      }
    ],
    "bugs": [],
    "type": null,
    "reference": null,
    "order": 0,
    "matrixOrder": 0,
    "id": 1331,
    "hash": 1331,
    "description": "some description",
    "status": "FAILED",
    "statusDescription": null,
    "startTime": "16:00",
    "finishTime": "16:00"
  }