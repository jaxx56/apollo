require 'java'
java_import 'org.apollo.game.model.Player'
java_import 'org.apollo.game.model.def.ItemDefinition'
java_import 'org.apollo.game.model.def.NpcDefinition'
java_import 'org.apollo.game.model.def.ObjectDefinition'

on :command, :lookup, RIGHTS_ADMIN do |player, command|
  args = command.arguments.to_a
  unless args.length > 1
    player.send_message("Invalid syntax - ::lookup [npc/object/item] [name]")
    return
  end
  
  type = args.shift.downcase
  name = args.join(" ").downcase

  if ['npc', 'object', 'item'].index(type) == nil
    player.send_message("Invalid syntax - ::lookup [npc/object/item] [name]") 
    return
  end

  Kernel.const_get("#{type.capitalize}Definition").definitions.each do |definition|
    if definition.name.to_s.downcase == name
      player.send_message("That #{type} has id #{definition.id}.")
      return
    end
  end

  player.send_message("Could not find an #{type} called #{name}.")
end

on :command, :iteminfo, RIGHTS_ADMIN do |player, command|
  args = command.arguments
  unless args.length == 1
    player.send_message("Invalid syntax - ::iteminfo [item id]")
    return
  end

  id = args[0].to_i
  definition = ItemDefinition.lookup(id)

  members = definition.is_members_only ? "members" : "not members"
  player.send_message("Item #{id} is called #{definition.name}, is #{members} only, and has a team of #{definition.team}.")
  player.send_message("Its description is \"#{definition.description}\".")
end

on :command, :npcinfo, RIGHTS_ADMIN do |player, command|
  args = command.arguments
  unless args.length == 1
    player.send_message("Invalid syntax - ::npcinfo [npc id]")
    return
  end

  id = args[0].to_i
  definition = NpcDefinition.lookup(id)

  is_combative = definition.has_combat_level ? "has a combat level of #{definition.combat_level}" : "does not have a combat level"
  player.send_message("Npc #{id} is called #{definition.name} and #{is_combative}.")
  player.send_message("Its description is \"#{definition.description}\".")
end