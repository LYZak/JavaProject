import email
from bs4 import BeautifulSoup
import quopri

def parse_mhtml(file_path):
    with open(file_path, 'rb') as f:
        msg = email.message_from_binary_file(f)

    for part in msg.walk():
        if part.get_content_type() == "text/html":
            content = part.get_payload(decode=True)
            # content might be bytes, decode to string if necessary
            # but beautifulsoup handles bytes well
            soup = BeautifulSoup(content, 'html.parser')
            
            # Find the main content area if possible, otherwise get all text
            # Moodle pages often have a specific id for content
            main_content = soup.find(role="main") or soup.find(id="region-main") or soup
            
            text = main_content.get_text(separator='\n', strip=True)
            return text
    return "No HTML content found"

try:
    file_path = r"d:\work\JavaProject\课程简称_10_ Project description.mhtml"
    text = parse_mhtml(file_path)
    print(text)
except Exception as e:
    print(f"Error: {e}")
