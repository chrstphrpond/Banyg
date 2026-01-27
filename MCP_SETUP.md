# Docker MCP Server Setup

## Overview

The Docker MCP (Model Context Protocol) Server has been installed and allows AI assistants to manage Docker containers, images, networks, and volumes directly.

## Installation Status

✅ **Installed**: `@0xshariq/docker-mcp-server` (v1.0.0)
📍 **Location**: `C:\Users\crit\AppData\Roaming\npm\docker-mcp-server`

## Available Commands

### Basic Docker Operations

| Command | Description | Example |
|---------|-------------|---------|
| `dimages` | List Docker images | `dimages` |
| `dps` | List running containers | `dps` |
| `dpsa` | List all containers | `dpsa` |
| `dpull <image>` | Pull Docker image | `dpull nginx` |
| `drun <image>` | Run Docker container | `drun nginx -p 80:80` |
| `dlogs <container>` | Show container logs | `dlogs mycontainer` |
| `dexec <container> <cmd>` | Execute command in container | `dexec mycontainer bash` |
| `dbuild <path>` | Build Docker image | `dbuild ./app` |

### Docker Compose Operations

| Command | Description | Example |
|---------|-------------|---------|
| `dup` | Docker Compose up | `dup` |
| `ddown` | Docker Compose down | `ddown` |
| `dcompose <cmd>` | General compose command | `dcompose logs` |

### Advanced Operations

| Command | Description | Example |
|---------|-------------|---------|
| `dnetwork <action>` | Manage networks | `dnetwork list` |
| `dvolume <action>` | Manage volumes | `dvolume create myvol` |
| `dinspect <type> <id>` | Inspect Docker objects | `dinspect container abc123` |
| `dprune [type]` | Remove unused objects | `dprune images` |

### Workflow Commands

| Command | Description |
|---------|-------------|
| `ddev <path> <name>` | Build and run development container |
| `dclean` | Clean up all unused Docker resources |
| `dstop` | Stop all running containers |
| `dreset` | Reset entire Docker environment |

## Usage with Kimi CLI

To use this MCP server with Kimi CLI, add to your `~/.kimi/config.json`:

```json
{
  "mcpServers": {
    "docker": {
      "command": "docker-mcp-server",
      "args": []
    }
  }
}
```

## Testing the Installation

Test the MCP server by running:

```bash
# List Docker images (Note: Windows users should use direct docker commands)
docker images

# List running containers
docker ps

# Use the docker-mcp-server for other operations
docker-mcp-server docker-pull nginx
```

### ⚠️ Windows Quoting Issue

There is a known issue with the format templates on Windows PowerShell. The following commands work directly with Docker:

```powershell
# Use these direct Docker commands instead
Docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}"
docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Names}}"
```

## Security Considerations

⚠️ **Warning**: This MCP server has full access to Docker daemon and can:
- Start/stop/delete containers
- Pull/build/delete images
- Manage networks and volumes
- Execute commands in containers

Use with caution in production environments.

## Troubleshooting

### Command not found
```powershell
# Add to PATH if needed
$env:PATH += ";C:\Users\crit\AppData\Roaming\npm"
```

### Docker permission issues
Make sure your user is in the `docker-users` group or run with appropriate permissions.

### Connection refused
Ensure Docker Desktop is running:
```powershell
docker version
```

## Uninstall

```bash
npm uninstall -g @0xshariq/docker-mcp-server
```

## Resources

- [MCP Documentation](https://modelcontextprotocol.io/)
- [Docker MCP Server GitHub](https://github.com/0xshariq/docker-mcp-server)
- [Docker CLI Reference](https://docs.docker.com/engine/reference/commandline/cli/)
