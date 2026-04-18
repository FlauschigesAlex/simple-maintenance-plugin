# Simple Maintenance Plugin
A simple solution to manage maintenance mode for your velocity server.

### Supported platforms and versions
- [Velocity](https://velocitypowered.com/) Versions: 3.4.0 - 3.5.0

Although simple-maintenance-plugin may work on other platforms or versions, I do not guarantee for their stability or functionality.

### Commands
- `/maintenance` - Lists all servers in maintenance mode.
- `/maintenance <server> (status)` - Displays the server's maintenance mode status.
- `/maintenance <server> status <status>` - Toggles the server's maintenance mode.
- `/maintenance <server> message <message>` - Sets the server's maintenance message.
- `/maintenance <server> message clear` - Resets the server's maintenance message.
- `/maintenance <server> duration indefinite` - Sets the server's maintenance duration to indefinite.
- `/maintenance <server> duration <duration>` - Sets the server's maintenance duration.
- `/maintenance <server> enforce` - Enforces the server's maintenance mode.

### Permissions
`smp.command`: Allows the user to use the `/maintenance` command.<br>
`smp.bypass`: Allows the user to bypass maintenance mode.