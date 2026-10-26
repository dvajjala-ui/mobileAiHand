"""Task templates for the phone actions we plan to verify on a real device."""

from __future__ import annotations

import json


FIELDS = {
    "reddit": ("community", "title", "body"),
    "whatsapp_photos": ("contact",),
    "uninstall": ("app",),
    "chatgpt": ("question",),
}


def compile_task(action: str, prompt: str, details: dict | None) -> tuple[str, str]:
    """Return a reviewable summary and the instruction sent to the phone agent."""
    prompt = prompt.strip()
    if len(prompt) > 2000:
        raise ValueError("Additional instructions are too long")
    if action == "general":
        if not prompt:
            raise ValueError("A phone task is required")
        return prompt, prompt
    if action not in FIELDS:
        raise ValueError("Unknown task type")
    if not isinstance(details, dict):
        raise ValueError("Task details are required")
    values = {}
    for name in FIELDS[action]:
        value = details.get(name)
        if not isinstance(value, str) or not value.strip() or len(value) > 2000:
            raise ValueError(f"Provide {name} (1–2000 characters)")
        values[name] = value.strip()
    if action == "reddit":
        community = values["community"].removeprefix("r/")
        if not community.replace("_", "").isalnum() or len(community) > 21:
            raise ValueError("Community must be a Reddit community name")
        values["community"] = community
        summary = f"Post to r/{community}: {values['title']}\n\n{values['body']}"
        instruction = (
            "Use the Reddit app on the selected Android phone to create a text post. "
            "Confirm the community, title, and body match these fields before publishing. "
            "After publishing, check for a visible post or link. If the app rejects the post, report that; never claim success without evidence."
        )
    elif action == "whatsapp_photos":
        summary = f"Send all photos taken today to WhatsApp contact: {values['contact']}"
        instruction = (
            "Use the Android gallery/photos app and WhatsApp on the selected phone. "
            "Find photos taken today according to the phone's local calendar date. "
            "Verify the exact WhatsApp contact and attachment count before sending. "
            "Send only today's photos; if the date or recipient cannot be verified, stop and report uncertainty. "
            "After sending, check the conversation for the attachments."
        )
    elif action == "uninstall":
        summary = f"Uninstall Android app: {values['app']}"
        instruction = (
            "On the selected Android phone, identify the exact installed app from its displayed name and package. "
            "Open its Android uninstall flow and follow the system confirmation. "
            "If multiple apps match, stop and ask which one. Verify that the app is no longer installed before reporting success."
        )
    else:
        summary = f"Ask in the ChatGPT app: {values['question']}"
        instruction = (
            "Use the installed ChatGPT app on the selected Android phone. Submit the exact question and report its reply. "
            "If the app is absent or not signed in, report that instead of answering from a different model."
        )
    serialized = json.dumps(values, ensure_ascii=False)
    instruction += f"\nTask fields (JSON): {serialized}"
    if prompt:
        summary += f"\nAdditional instructions: {prompt}"
        instruction += f"\nAdditional user instructions: {json.dumps(prompt, ensure_ascii=False)}"
    return summary, instruction
