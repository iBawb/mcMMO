package com.gmail.nossr50.skills.acrobatics;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.utilities.SkillTools;
import com.gmail.nossr50.skills.utilities.SkillType;
import com.gmail.nossr50.util.Permissions;

public class RollEventHandler extends AcrobaticsEventHandler {
    protected boolean isGraceful;
    private int damageThreshold;

    protected RollEventHandler(AcrobaticsManager manager, EntityDamageEvent event) {
        super(manager, event);

        isGracefulRoll();
        calculateSkillModifier();
        calculateDamageThreshold();
        calculateModifiedDamage();
    }

    @Override
    protected void calculateSkillModifier() {
        int skillModifer = manager.getSkillLevel();

        if (isGraceful) {
            skillModifer = skillModifer * 2;
        }

        skillModifer = SkillTools.skillCheck(skillModifer, Acrobatics.rollMaxBonusLevel);
        this.skillModifier = skillModifer;
    }

    @Override
    protected void calculateModifiedDamage() {
        int modifiedDamage = damage - damageThreshold;

        if (modifiedDamage < 0) {
            modifiedDamage = 0;
        }

        this.modifiedDamage = modifiedDamage;
    }

    @Override
    protected void modifyEventDamage() {
        event.setDamage(modifiedDamage);

        if (event.getDamage() == 0) {
            event.setCancelled(true);
        }
    }


    @Override
    protected void sendAbilityMessage() {
        Player player = manager.getMcMMOPlayer().getPlayer();

        if (isGraceful) {
            player.sendMessage(LocaleLoader.getString("Acrobatics.Ability.Proc"));
        }
        else {
            player.sendMessage(LocaleLoader.getString("Acrobatics.Roll.Text"));
        }
    }


    @Override
    protected void processXpGain(int xp) {
        manager.getMcMMOPlayer().beginXpGain(SkillType.ACROBATICS, xp);
    }

    /**
     * Check if this is a graceful roll.
     */
    private void isGracefulRoll() {
        Player player = manager.getMcMMOPlayer().getPlayer();

        if (Permissions.gracefulRoll(player)) {
            this.isGraceful = player.isSneaking();
        }
        else {
            this.isGraceful = false;
        }
    }

    /**
     * Calculate the damage threshold for this event.
     */
    private void calculateDamageThreshold() {
        int damageThreshold = 7;

        if (isGraceful) {
            damageThreshold = damageThreshold * 2;
        }

        this.damageThreshold = damageThreshold;
    }
}
