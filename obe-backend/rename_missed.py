import os
import re

def replace_in_string(content):
    # .programmeId(
    content = re.sub(r'\.programmeId\(', r'.masterProgrammeId(', content)
    # .getProgrammeId(
    content = re.sub(r'\.getProgrammeId\(', r'.getMasterProgrammeId(', content)
    # .setProgrammeId(
    content = re.sub(r'\.setProgrammeId\(', r'.setMasterProgrammeId(', content)
    
    # Same for Course
    content = re.sub(r'\.courseId\(', r'.masterCourseId(', content)
    content = re.sub(r'\.getCourseId\(', r'.getMasterCourseId(', content)
    content = re.sub(r'\.setCourseId\(', r'.setMasterCourseId(', content)

    # Batch
    content = re.sub(r'\.batchId\(', r'.programmeBatchId(', content)
    content = re.sub(r'\.getBatchId\(', r'.getProgrammeBatchId(', content)
    content = re.sub(r'\.setBatchId\(', r'.setProgrammeBatchId(', content)

    # CourseOffering
    content = re.sub(r'\.courseOfferingId\(', r'.programmeBatchCourseId(', content)
    content = re.sub(r'\.getCourseOfferingId\(', r'.getProgrammeBatchCourseId(', content)
    content = re.sub(r'\.setCourseOfferingId\(', r'.setProgrammeBatchCourseId(', content)

    return content

def process_directory(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                with open(filepath, 'r') as f:
                    content = f.read()
                
                new_content = replace_in_string(content)
                
                if new_content != content:
                    with open(filepath, 'w') as f:
                        f.write(new_content)

process_directory('src')
