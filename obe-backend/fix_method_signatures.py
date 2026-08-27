import os
import re

def clean_method_params(match):
    # Split by comma (ignoring commas inside annotations, but let's assume standard formatting)
    params_str = match.group(1)
    
    # Simple deduplication strategy:
    # We will keep track of variable names seen in this parameter list.
    param_tokens = params_str.split(',')
    
    seen_vars = set()
    new_params = []
    for token in param_tokens:
        # The variable name is typically the last word (ignoring array brackets etc., but we know they are Strings)
        match_var = re.search(r'\b(\w+)\s*$', token.strip())
        if match_var:
            var_name = match_var.group(1)
            if var_name in seen_vars:
                continue # Skip duplicate
            seen_vars.add(var_name)
        new_params.append(token)
        
    return '(' + ','.join(new_params) + ')'

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find method definitions: public/private/protected ... methodName( ... ) {
    # It might span multiple lines.
    
    # We use a regex that matches the parameter list of methods
    # e.g., @GetMapping(...) public ... method( ... )
    # This is tricky with regex, so we'll do something specific to the known duplicates:
    
    # Remove @RequestParam(...) String X, @RequestParam(...) String X
    content = re.sub(r'(@RequestParam[^)]*\)\s*String\s+masterProgrammeId)\s*,\s*@RequestParam[^)]*\)\s*String\s+masterProgrammeId', r'\1', content)
    content = re.sub(r'(@RequestParam[^)]*\)\s*String\s+programmeBatchId)\s*,\s*@RequestParam[^)]*\)\s*String\s+programmeBatchId', r'\1', content)
    content = re.sub(r'(@RequestParam[^)]*\)\s*String\s+programmeBatchCourseId)\s*,\s*@RequestParam[^)]*\)\s*String\s+programmeBatchCourseId', r'\1', content)
    content = re.sub(r'(@RequestParam[^)]*\)\s*String\s+masterCourseId)\s*,\s*@RequestParam[^)]*\)\s*String\s+masterCourseId', r'\1', content)
    
    # Also without @RequestParam
    content = re.sub(r'(String\s+masterProgrammeId)\s*,\s*String\s+masterProgrammeId\b', r'\1', content)
    content = re.sub(r'(String\s+programmeBatchId)\s*,\s*String\s+programmeBatchId\b', r'\1', content)
    content = re.sub(r'(String\s+programmeBatchCourseId)\s*,\s*String\s+programmeBatchCourseId\b', r'\1', content)
    content = re.sub(r'(String\s+masterCourseId)\s*,\s*String\s+masterCourseId\b', r'\1', content)

    # Some methods might just have the same param type and name twice
    # For instance: @RequestParam(name="masterProgrammeId") String masterProgrammeId, @RequestParam(required=false) String masterProgrammeId
    # Let's do a more robust one for masterProgrammeId
    content = re.sub(r'((?:@\w+\s*(?:\(.*?\))?\s*)*String\s+masterProgrammeId)\s*,\s*(?:@\w+\s*(?:\(.*?\))?\s*)*String\s+masterProgrammeId\b', r'\1', content)
    content = re.sub(r'((?:@\w+\s*(?:\(.*?\))?\s*)*String\s+programmeBatchId)\s*,\s*(?:@\w+\s*(?:\(.*?\))?\s*)*String\s+programmeBatchId\b', r'\1', content)
    content = re.sub(r'((?:@\w+\s*(?:\(.*?\))?\s*)*String\s+programmeBatchCourseId)\s*,\s*(?:@\w+\s*(?:\(.*?\))?\s*)*String\s+programmeBatchCourseId\b', r'\1', content)
    content = re.sub(r'((?:@\w+\s*(?:\(.*?\))?\s*)*String\s+masterCourseId)\s*,\s*(?:@\w+\s*(?:\(.*?\))?\s*)*String\s+masterCourseId\b', r'\1', content)

    with open(filepath, 'w') as f:
        f.write(content)

import glob
for filepath in glob.glob('src/main/java/com/dypiu/nba/controller/*.java'):
    process_file(filepath)

