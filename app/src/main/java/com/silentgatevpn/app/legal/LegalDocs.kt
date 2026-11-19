package com.silentgatevpn.app.legal

object LegalDocs {

    const val UPDATED = "19 November 2026"
    const val CONTACT = "[CONTACT: add your support email or GitHub Issues link here before publishing]"

    val privacy = LegalDoc(
        "privacy", "Privacy Policy", "What the app collects, stores and shares.",
        """
# Privacy Policy
Last updated: $UPDATED. Applies to SilentGate VPN ("the App", "we", "us").

# Summary
- The App has no accounts, no ads, no analytics SDKs and no crash reporters.
- We do not run servers that receive your traffic, browsing history, DNS queries or IP address.
- Settings and profiles stay on your device.
- Third-party services you choose to use (Cloudflare, DNS providers, your own Worker or server) have their own policies.

# 1. Data stored on your device
- Your settings (connection mode, DNS provider, split-tunnelling list, fragmentation values).
- Stealth profiles you import, including the server address and credentials inside them.
- Cloudflare WARP device credentials created by the networking core so the tunnel can reconnect.
- A short in-memory log of connection events, cleared when the App closes.
- The time and version of your acceptance of our legal documents.
This data is kept in the App's private storage. Uninstalling the App deletes it.

# 2. Data we do not collect
We do not collect your name, e-mail address, phone number, location, contacts, advertising ID, browsing history, traffic content, DNS queries or connection timestamps. We cannot see them and have nowhere to send them.

# 3. Network requests the App makes
When you connect or use the Tools tab, your device contacts third parties directly:
- Cloudflare (WARP registration and tunnel, connectivity check at 1.1.1.1).
- The encrypted DNS provider you select (Cloudflare, Quad9 or AdGuard).
- Diagnostic services used by the Tools tab and exit check: ipwho.is, edns.ip-api.com and api6.ipify.org. These see the IP address of your VPN exit, not your real address, when the tunnel is up.
- Any server named in a Stealth profile you import.
Those parties receive the information needed to answer the request and may log it under their own policies.

# 4. Permissions
- VPN service: required to route traffic through the tunnel.
- Network state: to follow changes between Wi-Fi and mobile data.
- Notifications: to show the persistent connection notice.
- App list (launcher apps only): to build the split-tunnelling list. The list never leaves your device.

# 5. Security
Traffic between your device and the tunnel endpoint is encrypted by the protocol in use. No VPN can guarantee anonymity, and the App cannot protect you if your device is compromised.

# 6. Retention and deletion
We hold no data about you. To delete local data, clear the App's storage or uninstall it.

# 7. Children
The App is for adults only. See the Age Requirement section of the Terms and Conditions.

# 8. International use
Because we operate no servers, no personal data is transferred by us. Third-party services may process data in other countries.

# 9. Your rights
Depending on your location you may have rights to access, correct or delete personal data. We hold none, so there is nothing to disclose. You may contact the third parties above for data they hold.

# 10. Changes
We may update this policy. The date at the top shows the latest version. Continued use after an update means you accept it.

# 11. Contact
$CONTACT
"""
    )

    val tos = LegalDoc(
        "tos", "Terms of Service", "The agreement between you and the developer.",
        """
# Terms of Service
Last updated: $UPDATED.

# 1. Agreement
By installing or using SilentGate VPN you agree to these Terms of Service, the Terms of Use, the Terms and Conditions, the Acceptable Use Policy, the Disclaimer and the Privacy Policy. If you do not agree, do not use the App.

# 2. What the service is
SilentGate VPN is a free client application that helps your device build an encrypted tunnel using public infrastructure (such as Cloudflare WARP) or servers that you configure. The developer does not operate a VPN network and does not sell access to one.

# 3. Free and as-is
The App is provided free of charge. There is no service-level commitment. Features, connectivity and third-party infrastructure can change or stop at any time.

# 4. Your account and responsibilities
There is no account. You are responsible for your device, your Stealth profiles and credentials, and everything you do while connected.

# 5. Third-party services
The tunnel depends on services that the developer does not control. Their availability, speed, terms and policies are theirs. See the Third-Party Services notice.

# 6. Your own servers and Workers
If you deploy a Cloudflare Worker or any server, you alone are responsible for that deployment, its cost, its compliance with the provider's terms and the data passing through it.

# 7. Suspension
You may stop using the App at any time. Access to third-party infrastructure may be limited or ended by those third parties without notice.

# 8. Changes to the service and terms
We may change the App or these Terms. Continued use after a change means you accept it.

# 9. Contact
$CONTACT
"""
    )

    val tou = LegalDoc(
        "tou", "Terms of Use", "How you may use the App and the licence you receive.",
        """
# Terms of Use
Last updated: $UPDATED.

# 1. Licence
You receive a personal, non-exclusive, non-transferable, revocable licence to install and use the App. The App's source code is published under the GNU General Public License v3.0 (see Open-Source Licenses). Where the GPL gives you more rights than these terms, the GPL prevails for the source code.

# 2. Permitted use
- Protecting your connection on networks you do not trust.
- Reaching lawful content and services.
- Testing, learning and research on your own devices and accounts.

# 3. Restrictions
You must not:
- Use the App for any activity that is unlawful where you are or where the traffic ends.
- Attack, scan or overload networks, servers or people.
- Send spam, malware or fraudulent traffic.
- Infringe intellectual property or privacy rights.
- Resell the App's access to third-party infrastructure or present it as an official Cloudflare product.
- Remove copyright, licence or attribution notices.

# 4. Local laws
VPN use is regulated differently around the world. You are responsible for knowing and following the laws that apply to you, including laws about encryption, circumvention tools, online safety and age restrictions.

# 5. Age
You must be at least 18 years old, or the age of majority where you live if higher.

# 6. Updates
The App may change or require updates to keep working.

# 7. Feedback
Feedback you send may be used to improve the App without obligation to you.

# 8. Termination
Your right to use the App ends automatically if you break these terms. Sections that by nature survive termination continue to apply.
"""
    )

    val terms = LegalDoc(
        "terms", "Terms and Conditions", "General conditions, liability and governing principles.",
        """
# Terms and Conditions
Last updated: $UPDATED.

# 1. Acceptance
These Terms and Conditions form one agreement together with the Terms of Service, Terms of Use, Acceptable Use Policy, Disclaimer and Privacy Policy. You accept them by ticking the box on first launch and by using the App.

# 2. Age requirement
The App is intended only for adults. You confirm that you are at least 18 years old (or older if your local age of majority is higher). Parents and guardians are responsible for the devices that minors use.

# 3. Ownership
The App's name, icon and design belong to the developer. Open-source components belong to their respective authors under their licences.

# 4. Limitation of liability
To the maximum extent permitted by applicable law, the developer is not liable for indirect, incidental, special, consequential or punitive damages, loss of data, loss of profit, service interruptions, blocked or throttled connections, account actions taken by third parties, or any outcome resulting from your use of the App. Where liability cannot be excluded, it is limited to the amount you paid for the App, which is zero.

# 5. Indemnity
You agree to be responsible for claims arising from your own unlawful use of the App or breach of these terms, to the extent permitted by law.

# 6. No guarantee of access or anonymity
Networks can detect, block or slow tunnels. We do not promise that any website or service will be reachable, that any exit location will stay the same, or that you will be anonymous.

# 7. Third-party content
We are not responsible for content, services or sites you access through the tunnel.

# 8. Export and sanctions
You must follow export-control and sanctions rules that apply to you.

# 9. Severability and waiver
If a part of these terms is unenforceable, the rest stays in force. Not enforcing a term is not a waiver of it.

# 10. Governing principles
These terms are to be interpreted under the law that applies to you as a consumer, and nothing here removes mandatory rights you have under that law. Disputes should first be raised through the contact below.

# 11. Changes
We may update these terms. The date at the top shows the latest version, and the App may ask you to accept a new version.

# 12. Contact
$CONTACT
"""
    )

    val disclaimer = LegalDoc(
        "disclaimer", "Disclaimer", "No warranties and important limits of the App.",
        """
# Disclaimer
Last updated: $UPDATED.

# 1. No warranty
The App is provided "as is" and "as available" without warranties of any kind, express or implied, including merchantability, fitness for a particular purpose, accuracy, availability and non-infringement.

# 2. Not a guarantee of privacy or security
A VPN changes where your traffic appears to come from. It does not make you anonymous, does not stop websites identifying you through accounts, cookies or fingerprinting, and does not protect against malware or phishing. Leak tests in the App are best-effort checks, not proof.

# 3. Kill switch
Android does not let an app promise a perfect kill switch. For the strongest protection, enable Always-on VPN and Block connections without VPN in Android settings.

# 4. Exit location and age-verification rules
The exit location check is informational. Exit locations come from third-party networks and can change. The App cannot guarantee that a site will or will not show an age-verification or regional notice.

# 5. Third-party infrastructure
Cloudflare WARP and other services are unofficial for this use, can change, rate-limit or stop working, and may breach their own terms if misused. Cloudflare Workers used as proxies may be restricted by Cloudflare's terms and can lead to account action. Use them at your own risk.

# 6. Proxy relays
Optional relay addresses (for example ProxyIPs) belong to third parties who may be able to see connection metadata. Content protected by TLS stays encrypted, but you are trusting that relay.

# 7. Legality
Using VPNs or circumvention tools is restricted in some places. You are responsible for compliance with local laws.

# 8. Not professional advice
Nothing in the App is legal, security or professional advice.

# 9. Experimental software
The App uses an experimental networking core and may contain bugs. Do not rely on it for safety-critical or life-critical purposes.
"""
    )

    val aup = LegalDoc(
        "aup", "Acceptable Use Policy", "What you must not do while connected.",
        """
# Acceptable Use Policy
Last updated: $UPDATED.

You agree not to use the App to:
- Break any law that applies to you or to the destination of the traffic.
- Harm, exploit or endanger children in any way. Any such use is strictly prohibited.
- Harass, stalk, threaten or defraud anyone.
- Distribute malware, run denial-of-service attacks, port-scan or probe systems without permission.
- Send bulk unsolicited messages.
- Infringe copyrights, trademarks or privacy rights.
- Evade bans or restrictions imposed on you personally for abuse.
- Abuse free infrastructure (for example create many WARP devices automatically, or run heavy resale of bandwidth).

Breaking this policy ends your licence to use the App. We may cooperate with lawful requests from authorities where we hold relevant information, which for this App is none beyond what is public in the open-source code.
"""
    )

    val data = LegalDoc(
        "data", "Data and Logging Statement", "What is and is not logged.",
        """
# Data and Logging Statement
Last updated: $UPDATED.

# What the developer logs
Nothing. The App has no developer-operated backend, so there are no server logs of your activity, IP address, DNS queries or traffic.

# What stays on your device
- A short rolling log of connection events held in memory only.
- Settings, imported profiles and WARP device credentials in private app storage.

# What third parties may log
- Cloudflare, when you use WARP or the connectivity check.
- Your selected DNS provider, for the queries it resolves.
- Any server or Worker you configure.
- Diagnostic services in the Tools tab.

# Statement limits
"No developer logs" describes the developer and the App only. It is not a promise about third-party providers, and it is not a claim of anonymity.
"""
    )

    val thirdParty = LegalDoc(
        "third", "Third-Party Services", "Services and components the App depends on.",
        """
# Third-Party Services
Last updated: $UPDATED.

The App is independent and is not affiliated with, endorsed by or sponsored by any company named below. Names belong to their owners.

# Services
- Cloudflare WARP and Cloudflare network: free tunnel infrastructure, connectivity check and optional Workers. Subject to Cloudflare's terms and privacy policy.
- Encrypted DNS: Cloudflare 1.1.1.1, Quad9 and AdGuard (unfiltered resolver). Subject to each provider's policy.
- ipwho.is, ip-api.com and ipify.org: used only for diagnostics.
- Your own servers or Cloudflare Workers, if you add a Stealth profile.

# Components
- Hiddify sing-box core (a fork of sing-box): networking engine with WireGuard, AmneziaWG, MASQUE, VLESS and TLS fragmentation.
- AmneziaWG and WireGuard implementations.
- Android Jetpack, Jetpack Compose and Material 3.
- Kotlin and kotlinx.coroutines.

# Notice
Third-party services may change, limit or end access at any time. We are not responsible for them.
"""
    )

    val oss = LegalDoc(
        "oss", "Open-Source Licenses", "GPL notice and component licences.",
        """
# Open-Source Licenses

# GNU General Public License v3.0
The networking core embedded in this App is derived from sing-box and the Hiddify sing-box fork, which are licensed under the GNU General Public License version 3 or later, with the additional terms stated in the upstream projects. As a combined work, SilentGate VPN is distributed under the GNU General Public License v3.0.

You have the right to receive, study, modify and share the corresponding source code of this App and of the networking core. The complete source is published by the developer in the project repository listed in the project README. You can rebuild the App yourself using the included GitHub Actions workflow.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details: https://www.gnu.org/licenses/gpl-3.0.html

# Other components
- AmneziaWG-go and WireGuard-go: MIT licence.
- gVisor: Apache License 2.0.
- uTLS (fork): BSD-3-Clause.
- Android Jetpack and Compose: Apache License 2.0.
- Kotlin and kotlinx.coroutines: Apache License 2.0.

Each component's full licence text is available in its upstream repository.
"""
    )

    val age = LegalDoc(
        "age", "Age Requirement and Children", "Adults only.",
        """
# Age Requirement and Children
Last updated: $UPDATED.

- SilentGate VPN is for people aged 18 or older, or the age of majority where they live if higher.
- You confirm your age on first launch. We do not collect or verify your age, and we do not collect any personal data.
- Parents and guardians should supervise devices used by minors and may use device-level parental controls.
- If you believe a minor is using the App, uninstall it from that device.
"""
    )

    val all: List<LegalDoc> = listOf(privacy, tos, tou, terms, disclaimer, aup, data, thirdParty, oss, age)
}
