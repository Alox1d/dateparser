#!/usr/bin/env python3
"""Validate the actual local KMP Maven artifacts before an immutable Central release."""
from pathlib import Path
import argparse, json, zipfile, xml.etree.ElementTree as ET
parser = argparse.ArgumentParser()
parser.add_argument("repository", type=Path)
parser.add_argument("--version", default="0.1.2")
parser.add_argument("--native", action="store_true")
args = parser.parse_args()
root = args.repository / "io/github/alox1d"
modules = ["dateparser-kotlin", "dateparser-kotlin-jvm", "dateparser-kotlin-android"]
if args.native: modules += ["dateparser-kotlin-iosarm64", "dateparser-kotlin-iossimulatorarm64"]
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
for module in modules:
    directory = root / module / args.version
    stem = module + "-" + args.version
    for suffix in (".pom", ".module", "-sources.jar", "-javadoc.jar"):
        if not (directory / (stem + suffix)).is_file(): raise SystemExit("Missing artifact: " + module + suffix)
    pom = ET.parse(directory / (stem + ".pom")).getroot()
    for field, expected in (("groupId", "io.github.alox1d"), ("artifactId", module), ("version", args.version)):
        if pom.findtext("m:" + field, namespaces=ns) != expected: raise SystemExit("Wrong POM " + field + ": " + module)
    if pom.findtext("m:licenses/m:license/m:name", namespaces=ns) != "MIT License": raise SystemExit("Missing MIT metadata")
    if pom.findtext("m:url", namespaces=ns) != "https://github.com/Alox1d/dateparser-kotlin": raise SystemExit("Wrong project URL")
    with zipfile.ZipFile(directory / (stem + "-javadoc.jar")) as jar:
        if not {"README.md", "README.ru.md", "THIRD_PARTY_NOTICES.md", "LICENSE"}.issubset(jar.namelist()): raise SystemExit("Bilingual documentation missing from " + module)
    metadata = json.loads((directory / (stem + ".module")).read_text())
    for variant in metadata.get("variants", []):
        for artifact in variant.get("files", []):
            if not (directory / artifact["url"]).is_file(): raise SystemExit("Missing variant file: " + artifact["url"])
    print("Validated", module)
tooling = root / "dateparser-kotlin" / args.version / ("dateparser-kotlin-" + args.version + "-kotlin-tooling-metadata.json")
json.loads(tooling.read_text())
print("KMP tooling metadata and", len(modules), "publication variants verified")
