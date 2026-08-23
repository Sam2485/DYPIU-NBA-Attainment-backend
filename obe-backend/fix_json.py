import re

with open('/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md', 'r') as f:
    text = f.read()

# Fix trailing commas before }
text = re.sub(r',\s*\}', '\n    }', text)
text = re.sub(r'\}', '}', text) # Just formatting

with open('/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md', 'w') as f:
    f.write(text)

