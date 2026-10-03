package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.PlayerAttachment;
import net.minecraft.world.entity.player.Player;

public class PlayerManager
{
    public static PlayerAttachment getPlayerData(Player player)
    {
        return player.getData(LithicClaimsAttachments.PLAYER_ATTACHMENT);
    }

    public static void saveAttachment(Player player, PlayerAttachment attachment)
    {
        player.setData(LithicClaimsAttachments.PLAYER_ATTACHMENT, attachment);
    }
}
