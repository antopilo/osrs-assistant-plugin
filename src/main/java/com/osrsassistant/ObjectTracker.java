package com.osrsassistant;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans the loaded scene for game objects and NPCs by name.
 * Used for precise navigation to interactable things (birdhouses, banks, NPCs, etc.)
 */
@Slf4j
public class ObjectTracker
{
	private final Client client;

	public ObjectTracker(Client client)
	{
		this.client = client;
	}

	/**
	 * Find the nearest game object matching the name (case-insensitive, partial match).
	 * Returns the WorldPoint of the nearest match, or null if not found.
	 */
	public WorldPoint findNearestObject(String name)
	{
		if (client.getLocalPlayer() == null) return null;

		WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		String searchLower = name.toLowerCase();
		WorldPoint nearest = null;
		int nearestDist = Integer.MAX_VALUE;

		try
		{
			Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
			int plane = client.getPlane();

			if (tiles == null || tiles.length <= plane) return null;

			for (int x = 0; x < tiles[plane].length; x++)
			{
				for (int y = 0; y < tiles[plane][x].length; y++)
				{
					Tile tile = tiles[plane][x][y];
					if (tile == null) continue;

					// Check game objects on this tile
					for (GameObject obj : tile.getGameObjects())
					{
						if (obj == null) continue;

						String objName = getObjectName(obj.getId());
						if (objName != null && objName.toLowerCase().contains(searchLower))
						{
							WorldPoint objPos = obj.getWorldLocation();
							int dist = playerPos.distanceTo(objPos);
							if (dist < nearestDist)
							{
								nearestDist = dist;
								nearest = objPos;
							}
						}
					}

					// Check ground objects
					TileObject groundObj = tile.getGroundObject();
					if (groundObj != null)
					{
						String objName = getObjectName(groundObj.getId());
						if (objName != null && objName.toLowerCase().contains(searchLower))
						{
							WorldPoint objPos = groundObj.getWorldLocation();
							int dist = playerPos.distanceTo(objPos);
							if (dist < nearestDist)
							{
								nearestDist = dist;
								nearest = objPos;
							}
						}
					}

					// Check wall objects
					TileObject wallObj = tile.getWallObject();
					if (wallObj != null)
					{
						String objName = getObjectName(wallObj.getId());
						if (objName != null && objName.toLowerCase().contains(searchLower))
						{
							WorldPoint objPos = wallObj.getWorldLocation();
							int dist = playerPos.distanceTo(objPos);
							if (dist < nearestDist)
							{
								nearestDist = dist;
								nearest = objPos;
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Error scanning scene objects: {}", e.getMessage());
		}

		if (nearest != null)
		{
			log.debug("Found object '{}' at {} (distance: {})", name, nearest, nearestDist);
		}

		return nearest;
	}

	/**
	 * Find the nearest NPC matching the name.
	 */
	public WorldPoint findNearestNpc(String name)
	{
		if (client.getLocalPlayer() == null) return null;

		WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		String searchLower = name.toLowerCase();
		WorldPoint nearest = null;
		int nearestDist = Integer.MAX_VALUE;

		var npcs = client.getTopLevelWorldView().npcs();
		for (NPC npc : npcs)
		{
			if (npc == null || npc.getName() == null) continue;

			if (npc.getName().toLowerCase().contains(searchLower))
			{
				WorldPoint npcPos = npc.getWorldLocation();
				int dist = playerPos.distanceTo(npcPos);
				if (dist < nearestDist)
				{
					nearestDist = dist;
					nearest = npcPos;
				}
			}
		}

		if (nearest != null)
		{
			log.debug("Found NPC '{}' at {} (distance: {})", name, nearest, nearestDist);
		}

		return nearest;
	}

	/**
	 * Find nearest object OR NPC by name.
	 */
	public WorldPoint findNearest(String name)
	{
		WorldPoint obj = findNearestObject(name);
		WorldPoint npc = findNearestNpc(name);

		if (obj == null) return npc;
		if (npc == null) return obj;

		// Return whichever is closer
		WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		return playerPos.distanceTo(obj) <= playerPos.distanceTo(npc) ? obj : npc;
	}

	private String getObjectName(int objectId)
	{
		try
		{
			ObjectComposition comp = client.getObjectDefinition(objectId);
			if (comp == null) return null;

			// Some objects have impostor compositions (transformed objects)
			if (comp.getImpostorIds() != null)
			{
				ObjectComposition impostor = comp.getImpostor();
				if (impostor != null)
				{
					return impostor.getName();
				}
			}

			String name = comp.getName();
			return (name != null && !name.equals("null")) ? name : null;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
