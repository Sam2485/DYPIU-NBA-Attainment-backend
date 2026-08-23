import re
import os

files = [
    '/Users/rajshaikh.gemini/antigravity-cli/brain/6c94e0a5-e10b-4215-a2f8-3316107ff5ea/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
]

def process(text):
    text = re.sub(
        r'"table2Direct": \[\n\s*\{\n\s*"outcomeCode": "string",\n\s*"averageMapping": 0,\n\s*"directContribution": 0\n\s*\}\n\s*\]',
        r'''"table2DirectPO": [
        {
          "poCode": "string",
          "averageMapping": 0,
          "directContribution": 0
        }
      ],
      "table2DirectPSO": [
        {
          "psoCode": "string",
          "averageMapping": 0,
          "directContribution": 0
        }
      ]''', text)
    
    text = re.sub(
        r'"report1AverageMapping": \[\n\s*\{\n\s*"outcomeCode": "string",\n\s*"semesterAverages": \[\n\s*\{\n\s*"semester": "string",\n\s*"value": 0\n\s*\}\n\s*\],\n\s*"programmeAverageMapping": 0\n\s*\}\n\s*\]',
        r'''"report1AverageMappingPO": [
      {
        "poCode": "string",
        "semesterAverages": [
          {
            "semester": "string",
            "value": 0
          }
        ],
        "programmeAverageMapping": 0
      }
    ],
    "report1AverageMappingPSO": [
      {
        "psoCode": "string",
        "semesterAverages": [
          {
            "semester": "string",
            "value": 0
          }
        ],
        "programmeAverageMapping": 0
      }
    ]''', text)

    text = re.sub(
        r'"report2DirectAttainment": \[\n\s*\{\n\s*"outcomeCode": "string",\n\s*"semesterDirectAttainments": \[\n\s*\{\n\s*"semester": "string",\n\s*"value": 0\n\s*\}\n\s*\],\n\s*"programmeDirectAttainment": 0\n\s*\}\n\s*\]',
        r'''"report2DirectAttainmentPO": [
      {
        "poCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "string",
            "value": 0
          }
        ],
        "programmeDirectAttainment": 0
      }
    ],
    "report2DirectAttainmentPSO": [
      {
        "psoCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "string",
            "value": 0
          }
        ],
        "programmeDirectAttainment": 0
      }
    ]''', text)

    text = re.sub(
        r'"report3IndirectAttainment": \[\n\s*\{\n\s*"outcomeCode": "string",\n\s*"percentageSubstantial": 0,\n\s*"percentageModerate": 0,\n\s*"percentageSlight": 0,\n\s*"weightedScore": 0,\n\s*"indirectPercentage": 0,\n\s*"indirectAttainmentLevel": 0\n\s*\}\n\s*\]',
        r'''"report3IndirectAttainmentPO": [
      {
        "poCode": "string",
        "percentageSubstantial": 0,
        "percentageModerate": 0,
        "percentageSlight": 0,
        "weightedScore": 0,
        "indirectPercentage": 0,
        "indirectAttainmentLevel": 0
      }
    ],
    "report3IndirectAttainmentPSO": [
      {
        "psoCode": "string",
        "percentageSubstantial": 0,
        "percentageModerate": 0,
        "percentageSlight": 0,
        "weightedScore": 0,
        "indirectPercentage": 0,
        "indirectAttainmentLevel": 0
      }
    ]''', text)
    
    text = re.sub(
        r'"report4OverallAttainment": \[\n\s*\{\n\s*"outcomeCode": "string",\n\s*"statement": "string",\n\s*"targetLevel": 0,\n\s*"directAttainment": 0,\n\s*"indirectAttainment": 0,\n\s*"finalAttainment": 0,\n\s*"targetMet": true,\n\s*"observation": "string"\n\s*\}\n\s*\]',
        r'''"report4OverallAttainmentPO": [
      {
        "poCode": "string",
        "statement": "string",
        "targetLevel": 0,
        "directAttainment": 0,
        "indirectAttainment": 0,
        "finalAttainment": 0,
        "targetMet": true,
        "observation": "string"
      }
    ],
    "report4OverallAttainmentPSO": [
      {
        "psoCode": "string",
        "statement": "string",
        "targetLevel": 0,
        "directAttainment": 0,
        "indirectAttainment": 0,
        "finalAttainment": 0,
        "targetMet": true,
        "observation": "string"
      }
    ]''', text)

    return text

for file in files:
    if os.path.exists(file):
        with open(file, 'r') as f:
            text = f.read()
        text = process(text)
        with open(file, 'w') as f:
            f.write(text)

