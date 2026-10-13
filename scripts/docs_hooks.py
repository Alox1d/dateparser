"""Set the server-rendered language on each localized documentation page."""
def on_post_page(output, page, config):
    language = "ru" if page.file.src_uri.startswith("ru/") else "en"
    return output.replace('<html lang="en"', '<html lang="' + language + '"', 1)
