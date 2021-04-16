import sys
import os
import io
import unicodedata
import requests
from PIL import Image
import json
import codecs
import re
import time
from collections import OrderedDict

api_key="476b719f9d7f724b732c8b82bbea162df7a45baf"
EnableRename=False
minsim='65!'#forcing minsim to 80 is generally safe for complex images, but may miss some edge cases. If images being checked are primarily low detail, such as simple sketches on white paper, increase this to cut down on false positives.

extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"}
thumbSize = (250,250)
url = 'http://saucenao.com/search.php?output_type=2&numres=1&minsim='+minsim+'&dbmask='+str(16275849184)+'&api_key='+api_key

proxies = {
    "http": "http://127.0.0.1:10809",
    "https": "http://127.0.0.1:10809"
}

image = Image.open(sys.argv[1])
image = image.convert("RGB")
image.thumbnail(thumbSize, resample = Image.ANTIALIAS)
imageData = io.BytesIO()
image.save(imageData,format='PNG')

files = {'file': ("upload.png", imageData.getvalue())}
imageData.close()

for i in range(3):
    response = requests.post(url, files=files, proxies=proxies)
    if response.status_code == 200:
        # print(response.text)
        # sys.exit(200)
        break

    if response.status_code == 403:
        # print("[API Auth Error] Invalid API Key")
        sys.exit(403)
        break
    
results = json.JSONDecoder(object_pairs_hook=OrderedDict).decode(response.text)

if int(results["header"]["user_id"]) > 0:
    print(str(results["header"]["short_remaining"]))
    print(str(results["header"]["long_remaining"]))

    if float(results['results'][0]['header']['similarity']) > float(results['header']['minimum_similarity']):
        
        print(str(results['results'][0]['header']['similarity']))

        database = ""
        illust_id = ""
        member_id = ""
        member_name = ""
        title = ""
        index_id = results['results'][0]['header']['index_id']
        extern_url = results["results"][0]["data"]["ext_urls"]

        if index_id == 5 or index_id == 6:
            database = "Pixiv"
            member_id = results['results'][0]['data']['member_id']
            illust_id=results['results'][0]['data']['pixiv_id']
            member_name = results["results"][0]["data"]["member_name"]
            title = results["results"][0]["data"]["title"]
        elif index_id == 8:
            database = "Seiga"
            member_id = results['results'][0]['data']['member_id']
            illust_id=results['results'][0]['data']['seiga_id']
        elif index_id == 10:
            database = "Drawr Images"
            member_id = results['results'][0]['data']['member_id']
            illust_id=results['results'][0]['data']['drawr_id']
        elif index_id == 11:
            database = "Nijie Images"
            member_id = results['results'][0]['data']['member_id']
            illust_id=results['results'][0]['data']['nijie_id']
        elif index_id == 34:
            database = "deviantArt"
            illust_id=results['results'][0]['data']['da_id']

        else:
            database = "未知的数据库"
            member_id = illust_id = "NaN"

id = results['results'][0]['data']["pixiv_id"]
url = 'https://www.pixiv.net/ajax/illust/{}/pages?lang=zh'.format(id)

headers = {
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36',
    'referer': 'https://www.pixiv.net/artworks/82505946'
}

response = requests.get(url, headers=headers, proxies=proxies).text
res_json = json.loads(response)

body = res_json['body']
download_url = body[0]['urls']['original']
name = download_url.split('/')[-1]

content = requests.get(download_url, headers = headers, proxies = proxies).content

with open(name, mode="wb") as f:
    f.write(content)

print(database)
if database == "Pixiv":
    print(member_name)
    print(title)
    print(name)
print(str(extern_url)[2:-2])
print(id)

sys.exit(200)