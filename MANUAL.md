# TermBot Manual

This manual covers the primary operator flows that are already implemented and smoke-tested.

## What Ships Now

- Direct SSH hosts
- SSH `Jump via host`
- YubiKey-backed SSH authentication with OpenPGP, PIV, and FIDO2
- Direct AWS SSM shell sessions
- AWS SSM with temporary session credentials
- AWS SSM with `AssumeRole`
- AWS SSM with MFA through AWS STS
- AWS SSM local port forwarding to the managed node or to a remote host behind it
- SSH `Route via SSM host`
- Combined `Route via SSM host` + `Jump via host`
- Encrypted backup/export and restore
- In-app debug report export

## Before You Start

- For SSH targets, create and smoke the direct host first when possible.
- For SSM, make sure the target instance is SSM-managed and reachable by AWS Systems Manager.
- For SSM MFA, use a code-based MFA device supported by AWS STS. AWS STS does not accept FIDO2/passkey MFA assertions.
- If you enable `Remember password`, TermBot stores the base secret path for reuse. Assumed-role credentials remain runtime-only.

## Direct SSH Host

1. Add a host.
2. Set `Protocol` to `ssh`.
3. Fill the target address, port, and username.
4. Choose the auth path you need:
   - password
   - saved secret
   - public key
   - YubiKey-backed key if you already imported or generated it
5. Save and connect.

Use this for normal direct SSH without an SSH jump host or SSM routing.

## SSH Jump Via Host

1. Create and verify the jump SSH host first.
2. Edit the final SSH target host.
3. Set `Jump via host` to the saved SSH jump host.
4. Save and connect to the final target.

TermBot will connect to the jump host first and then forward to the final SSH target.

## Direct SSM Shell

1. Add a host.
2. Set `Protocol` to `ssm`.
3. Fill these SSM fields:
   - `AWS access key ID`
   - `AWS region`
   - `SSM target`
4. Save and connect.
5. Enter the `AWS secret access key` when prompted.

Notes:
- `SSM target` is usually an EC2 instance ID such as `i-0123456789abcdef0`.
- If the access key is temporary (`ASIA...`), TermBot will also prompt for `AWS session token`.

## SSM With Role ARN

1. Start from a working SSM host.
2. Fill `AWS role ARN`.
3. Save and connect.

TermBot uses the base AWS credentials to call STS `AssumeRole`, then starts the SSM session with the derived role credentials.

What is stored:
- base secret path: optionally, if you enable `Remember password`
- assumed-role credentials: never persisted

## SSM With MFA

1. Start from a working SSM host or SSM+role host.
2. Fill `MFA device serial or ARN`.
3. Save and connect.
4. Enter the MFA code when prompted.

Behavior:
- direct SSM with long-lived IAM-user keys uses STS `GetSessionToken`
- SSM with `AWS role ARN` uses STS `AssumeRole` with MFA parameters

Important:
- Current MFA support is the realistic AWS STS model: `SerialNumber` + `TokenCode`
- AWS console passkeys and FIDO2 security keys are not accepted by STS for this API flow

## SSM Port Forwarding

Use this when the managed SSM node can reach another private service, such as a database.

1. Open the working SSM host and connect it first.
2. Open `Port forwards` for that SSM host.
3. Add a `Local` forward.
4. Set:
   - `Source port`: the phone-local port clients will use
   - `Destination`: the target from the managed node point of view

Examples:
- Managed node port:
  - source `15432`
  - destination `localhost:22`
- Private database behind the managed node:
  - source `15433`
  - destination `db.internal:5432`

After the forward is enabled:
- connect the client on the phone to `127.0.0.1:<source port>`

Current scope:
- only `Local` forwards are supported for SSM in this implementation

## SSH Via SSM Route Host

Use this when the final SSH target is private, but reachable from an SSM-managed node.

1. Create and verify the SSM host first.
2. Create or edit the SSH host for the final target.
3. Set `Route via SSM host` to the saved SSM host.
4. Save and connect.

TermBot automatically creates the SSM tunnel for the SSH session. You do not need to create a manual SSM port forward for this path.

## SSH Via SSM Route Host Plus SSH Jump Host

Use this when the final SSH target is behind both:
- an SSM-managed routing node
- a separate SSH jump host reachable from that managed node

Setup:
1. Create and verify the SSM route host.
2. Create and verify the SSH jump host.
3. Edit the final SSH target host.
4. Set:
   - `Route via SSM host` = the SSM route host
   - `Jump via host` = the SSH jump host
5. Save and connect.

Execution order:
1. SSM route tunnel
2. SSH jump host
3. final SSH target

This combined path has already been operator-smoked.

## Encrypted Backup And Restore

Use this to move hosts and saved state to another device or reinstall.

What is covered:
- SSH hosts
- SSM hosts
- SSM target metadata
- saved scoped SSM secrets
- route-host and jump-host references

What is not persisted:
- assumed-role temporary credentials
- MFA-derived STS session credentials

Recommendation:
- export before major device or app changes
- test restore on a secondary environment before relying on it operationally

## Export Debug Report

Use this for failures instead of trying to guess what happened.

1. Reproduce the issue.
2. Export the debug report from the app.
3. Capture the exact local failure time.
4. Share the exported report file.

This is the primary diagnostic channel for SSH, YubiKey, and SSM issues.

## Current Limits

- AWS IAM Identity Center is out of scope.
- AWS STS MFA support is code-based only. Console passkeys/security keys are not supported for this API path.
- YubiKey OATH/TOTP convenience integration is not implemented.
- SSM port forwarding currently supports `Local` forwards only.
