import os
import re

def fix_content(content):
    # JSON payload keys
    content = re.sub(r'\"programmeId\"', '"masterProgrammeId"', content)
    content = re.sub(r'\"batchId\"', '"programmeBatchId"', content)
    content = re.sub(r'\"courseId\"', '"masterCourseId"', content)
    content = re.sub(r'\"courseOfferingId\"', '"programmeBatchCourseId"', content)
    
    # URL strings
    content = re.sub(r'/academic/programmes', '/academic/master-programmes', content)
    content = re.sub(r'/academic/batches', '/academic/programme-batches', content)
    content = re.sub(r'/academic/courses', '/academic/master-courses', content)
    content = re.sub(r'/academic/course-offerings', '/academic/programme-batch-courses', content)
    
    # URL Query params
    content = re.sub(r'\?programmeId=', '?masterProgrammeId=', content)
    content = re.sub(r'\?batchId=', '?programmeBatchId=', content)
    content = re.sub(r'\?courseId=', '?masterCourseId=', content)
    content = re.sub(r'\?courseOfferingId=', '?programmeBatchCourseId=', content)
    
    content = re.sub(r'&programmeId=', '&masterProgrammeId=', content)
    content = re.sub(r'&batchId=', '&programmeBatchId=', content)
    content = re.sub(r'&courseId=', '&masterCourseId=', content)
    content = re.sub(r'&courseOfferingId=', '&programmeBatchCourseId=', content)

    # Some test paths using variables e.g., "/academic/programmes/" + id
    content = re.sub(r'\"/academic/programmes/\"', '"/academic/master-programmes/"', content)
    content = re.sub(r'\"/academic/batches/\"', '"/academic/programme-batches/"', content)
    content = re.sub(r'\"/academic/courses/\"', '"/academic/master-courses/"', content)
    content = re.sub(r'\"/academic/course-offerings/\"', '"/academic/programme-batch-courses/"', content)
    
    return content

def process_directory(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                with open(filepath, 'r') as f:
                    content = f.read()
                
                new_content = fix_content(content)
                
                if new_content != content:
                    with open(filepath, 'w') as f:
                        f.write(new_content)
                        print(f"Fixed {filepath}")

process_directory('src/test')
