package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.Plugin;

import java.util.*;

/**
 * Manages navigation using the Shortest Path plugin.
 * If Shortest Path is not installed, navigation targets are still tracked
 * but no path is rendered.
 */
@Slf4j
public class NavigationManager
{
	private final Client client;
	private final EventBus eventBus;
	private final PluginManager pluginManager;

	private WorldPoint destination;
	private String destinationName;
	private Boolean shortestPathAvailable = null;
	private int recheckCounter = 0;

	private static final int ARRIVAL_DISTANCE = 5;
	private static final int RECHECK_INTERVAL = 100;
	private static final String SP_NAMESPACE = "shortestpath";

	public NavigationManager(Client client, EventBus eventBus, PluginManager pluginManager)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.pluginManager = pluginManager;
	}

	private boolean isShortestPathAvailable()
	{
		recheckCounter++;
		if (shortestPathAvailable != null && recheckCounter < RECHECK_INTERVAL)
		{
			return shortestPathAvailable;
		}
		recheckCounter = 0;

		boolean found = false;
		try
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (plugin.getClass().getSimpleName().equals("ShortestPathPlugin"))
				{
					found = pluginManager.isPluginEnabled(plugin);
					break;
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Error checking for Shortest Path: {}", e.getMessage());
		}

		if (shortestPathAvailable == null || found != shortestPathAvailable)
		{
			shortestPathAvailable = found;
			if (!found)
			{
				log.info("Shortest Path plugin not found. Install it for in-game path rendering.");
			}
		}

		return shortestPathAvailable;
	}

	public void navigateTo(WorldPoint dest, String name)
	{
		this.destination = dest;
		this.destinationName = name;

		if (isShortestPathAvailable())
		{
			sendToShortestPath(dest);
		}
	}

	public void navigateTo(WorldPoint dest)
	{
		navigateTo(dest, null);
	}

	public void navigateSequential(List<WorldPoint> destinations, List<String> names)
	{
		if (destinations.isEmpty()) return;

		this.destination = destinations.get(0);
		this.destinationName = (names != null && !names.isEmpty()) ? names.get(0) : null;

		if (isShortestPathAvailable())
		{
			sendToShortestPathSequential(destinations);
		}
	}

	public void cancel()
	{
		this.destination = null;
		this.destinationName = null;

		if (shortestPathAvailable != null && shortestPathAvailable)
		{
			clearShortestPath();
		}
	}

	public boolean isNavigating()
	{
		return destination != null;
	}

	public String getDestinationName()
	{
		return destinationName;
	}

	public WorldPoint getDestination()
	{
		return destination;
	}

	public int getDistanceToDestination()
	{
		if (destination == null || client.getLocalPlayer() == null) return -1;
		return client.getLocalPlayer().getWorldLocation().distanceTo(destination);
	}

	public void update()
	{
		if (destination == null || client.getLocalPlayer() == null)
		{
			return;
		}

		int distance = client.getLocalPlayer().getWorldLocation().distanceTo(destination);
		if (distance <= ARRIVAL_DISTANCE)
		{
			log.info("Arrived at {}", destinationName);
			cancel();
		}
	}

	private void sendToShortestPath(WorldPoint target)
	{
		try
		{
			Map<String, Object> data = new HashMap<>();
			data.put("target", target);
			eventBus.post(new PluginMessage(SP_NAMESPACE, "path", data));
		}
		catch (Exception e)
		{
			log.warn("Failed to send to Shortest Path: {}", e.getMessage());
			shortestPathAvailable = false;
		}
	}

	private void sendToShortestPathSequential(List<WorldPoint> targets)
	{
		try
		{
			Set<Object> targetSet = new LinkedHashSet<>(targets);
			Map<String, Object> data = new HashMap<>();
			data.put("target", targetSet);
			eventBus.post(new PluginMessage(SP_NAMESPACE, "path", data));
		}
		catch (Exception e)
		{
			log.warn("Failed to send sequential targets: {}", e.getMessage());
			shortestPathAvailable = false;
		}
	}

	private void clearShortestPath()
	{
		try
		{
			eventBus.post(new PluginMessage(SP_NAMESPACE, "clear"));
		}
		catch (Exception e)
		{
			log.debug("Failed to clear Shortest Path: {}", e.getMessage());
		}
	}
}
