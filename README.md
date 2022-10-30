![Header](./docs/banner.png)
# Parabox

[![Release](https://img.shields.io/github/v/release/Parabox-App/Parabox)](https://github.com/Parabox-App/Parabox/releases)
![stars](https://img.shields.io/github/stars/Parabox-App/Parabox)
[![Telegram](https://img.shields.io/badge/Join-Telegram-red)](https://t.me/parabox_support)
![license](https://img.shields.io/github/license/Parabox-App/Parabox)

An instant messaging client with friendly interface, complete functions, and extensibility.

[简体中文](./README_zh_cn.md) / [Offical Website](https://parabox.ojhdt.dev/) / [User Document](https://docs.parabox.ojhdt.dev) / [Developer Document](https://docs.parabox.ojhdt.dev/developer/)

## Feature

### Flexible
It fits your usage needs, breaks the barrier of information sources, and conducts secondary screening, classification, and grouping of conversations and content at will. Let chat return to pure.
### Personality
The interface design follows Google's new design language - Material You. The theme is generated according to the wallpaper color (Android 12 only), with a variety of built-in color themes to choose from, just designed for you.

Layout adaptation for multiple screen sizes (mobile phones, foldable devices, tablets) and support for dark mode.
### Synchronize
Use Firebase Cloud Messaging to build a message synchronization network between different devices. Easily transfer the cost of message reception and greatly save background overhead.
### Plugin
Third-party plugins provide stable news sources. Users can build their own message source according to their usage habits. Messages from different sources can still be processed uniformly and efficiently.
### Follow best practices
Use MAD skills. The interface is built entirely using Jetpack Compose, the new toolkit for native Android interfaces. Jetpack libraries (including but not limited to `Paging 3` , `DataStore` , `Navigation` , `WorkManager` , `Room`) are used in persistence, navigation, background scheduling, architecture and more.

## Function

- Messaging: Supports the receiving and sending of common message types.
- Session Grouping: Group different sessions from different platforms into a new session.
- File management: independent file management page. Provides various filter conditions such as time, type, file size, etc.
- File Cloud Backup: Automatically back up the specified session files to the cloud, making it hard to miss and never lose.
- Notification evolution: notification channel, expand notification, quick reply, dialogue bubble
- System-level push: Use FCM to form a message synchronization network.
- Plug-in: third-party plug-ins provide richer and more stable information sources.
- Data Export: Export session data to local storage.

## Screenshot

![light](./docs/light.png)

![dark](./docs/dark.png)

## Installation

<a href='https://play.google.com/store/apps/details?id=com.ojhdtapp.parabox&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en/badges/static/images/badges/en_badge_web_generic.png' width='250'/></a>

Or visit [Releases](https://github.com/Parabox-App/Parabox/releases) to get the latest version.

## Get Help

📧 [Mail](mailto:parabox@ojhdt.dev) 

💬 [Telegram](https://t.me/parabox_support)

## Contributors

[![](https://contrib.rocks/image?repo=Parabox-App/Parabox)](https://github.com/Parabox-App/Parabox/graphs/contributors)
## License
```
MIT License

Copyright (c) 2022 ojhdt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```