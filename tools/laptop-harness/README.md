# Laptop harness (development reference)

This was the first experiment before the phone-only requirement was specified. The primary app and setup guide are now in the [root README](../../README.md). This harness remains available for comparing Mobilerun against the local Android agent on a test device.

## Prepare a laptop and Android phone

Use Python 3.11–3.13 and [uv](https://docs.astral.sh/uv/). On a laptop with a writable user directory:

```sh
uv sync --frozen
uv run mobilerun doctor
uv run mobilerun setup
uv run mobilerun ping
uv run mobilerun configure
uv run mobilerun devices
```

Enable Developer options and USB debugging on your Android phone and connect it over USB for the first setup. `mobilerun setup` installs Portal and helps enable its accessibility service. `mobilerun configure` chooses an AI model provider and its authentication method; enter credentials through that tool, not into the repository. Run a basic device check before using the web interface:

```sh
uv run mobilerun run --device YOUR_DEVICE_ID "Open Settings and tell me the Android version"
```

Start the prompt interface:

```sh
uv run mobileaihand --device YOUR_DEVICE_ID
```

Choose the intended phone from `mobilerun devices` and start the interface with `uv run mobileaihand --device YOUR_DEVICE_ID`. Phone tasks require this explicit device selection, so another connected phone is not chosen by accident. Open the address printed by the server on the laptop and enter its access token. Each prompt has a separate review step before the agent starts. The server handles one phone task at a time. A zero exit code means Mobilerun finished; check its report and the phone to confirm the requested result.

For a single task from the laptop terminal, use `uv run mobileaihand --device YOUR_DEVICE_ID "Open Settings and tell me the Android version"`. It shows the request and asks you to approve it before starting.

To open the interface from your phone while both devices are on a trusted local network, run `uv run mobileaihand --device YOUR_DEVICE_ID --host 0.0.0.0 --allow-http-lan`, then visit `http://<laptop-LAN-IP>:8765` on the phone. This sends the access token over plain HTTP on that network. Use a private network and HTTPS proxy for remote access; do not expose the server directly to the public Internet. The phone running Portal is the device being controlled; the browser is only the prompt interface.

The cloud workspace has no paired phone or emulator, so it can run the web tests but cannot verify live Android actions. Its home directory is read-only to ADB; use a laptop with normal Android platform tools and a writable user directory for the first device trial.

## Local checks

```sh
uv run python -m unittest discover -s tests -v
```

The web server itself uses Python's standard library. Mobilerun 0.7.2 currently needs `adbutils` for its device command, so both packages are declared explicitly.
