import re
import os

files = [
    '/Users/rajshaikh.gemini/antigravity-cli/brain/6c94e0a5-e10b-4215-a2f8-3316107ff5ea/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
]

def process(text):
    text = re.sub(
        r'"poIndirectAttainment": \[\n\s*\{\n\s*"poCode": "string",\n\s*"psoCode": "string",\n\s*"indirectAttainment": "number"\n\s*\}\n\s*\],\n\s*"psoIndirectAttainment": \[\n\s*\{\n\s*"poCode": "string",\n\s*"psoCode": "string",\n\s*"indirectAttainment": "number"\n\s*\}\n\s*\]',
        r'''"poAttainment": [
      {
        "poCode": "string",
        "weightedScore": "number"
      }
    ],
    "psoAttainment": [
      {
        "psoCode": "string",
        "weightedScore": "number"
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

