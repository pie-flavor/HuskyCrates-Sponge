package com.codehusky.huskycrates.crate.virtual.views;

import com.codehusky.huskycrates.crate.physical.PhysicalCrate;
import com.codehusky.huskycrates.crate.virtual.Crate;
import com.codehusky.huskycrates.crate.virtual.Item;
import com.codehusky.huskyui.StateContainer;
import com.codehusky.huskyui.states.Page;
import com.codehusky.huskyui.states.element.Element;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Random;
import java.util.function.Consumer;

public class SpinnerView implements Consumer<Page> {
    private Location<World> physicalLocation;
    private Crate crate;
    private int selectedSlot;
    private Player player;
    private Config config;

    public SpinnerView(PhysicalCrate pcrate, Player player){
        this.crate = pcrate.getCrate();
        this.physicalLocation = pcrate.getLocation();
        this.config = (Config) crate.getViewConfig();
        this.variance = (int)Math.round(new Random().nextDouble() * config.getTicksToSelectionVariance());
        this.selectedSlot = crate.selectSlot();
        this.player = player;
        Page.PageBuilder builder =
            Page.builder()
                .setAutoPaging(false)
                .setTitle(TextSerializers.FORMATTING_CODE.deserialize(crate.getName()))
                .setUpdatable(true)
                .setUpdater(this)
                .setInterrupt(() -> {
                    if(!rewardGiven) {
                        crate.getSlot(selectedSlot).rewardPlayer(player,this.physicalLocation);
                        player.playSound(SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP, player.getLocation().getPosition(), 0.5);
                    }
                })
                .setInventoryDimension(InventoryDimension.of(9,3));

        Element borderElement = new Element(config.getBorderItem().toItemStack());

        Element selectorItem = new Element(config.getSelectorItem().toItemStack());

        for(int i = 0; i < 9*3; i++){
            builder.putElement(i,(i == 4 | i == 22)? selectorItem : borderElement);
        }
        Page page = builder.build("meme");
        StateContainer sc = new StateContainer();
        sc.setInitialState(page);
        sc.launchFor(player);
    }

    int spinnerOffset = 0;
    int currentTicks = 0;
    double currentTickDelay = 1;
    int variance = 0;
    boolean rewardGiven = false;

    boolean hasWon = false;
    long tickWinBegin = 0;

    private boolean winCondition() {
        return spinnerOffset + variance >= config.getTicksToSelection();
    }

    private ItemStack getConfetti() {
        DyeColor[] colors = {DyeColors.BLUE,DyeColors.CYAN,DyeColors.LIGHT_BLUE,DyeColors.LIME,DyeColors.MAGENTA,DyeColors.ORANGE,DyeColors.PINK,DyeColors.PURPLE,DyeColors.RED, DyeColors.YELLOW};
        ItemStack g =ItemStack.builder()
                .itemType(ItemTypes.STAINED_GLASS_PANE)
                .add(Keys.DYE_COLOR,colors[(int)Math.floor(Math.random() * colors.length)])
                .build();
        g.offer(Keys.DISPLAY_NAME, Text.of(TextStyles.RESET,"You win!"));
        return g;
    }

    @Override
    public void accept(Page page) {
        if(winCondition() && !hasWon){
            hasWon = true;
            tickWinBegin = page.getTicks();
        }
        if(!hasWon) {
            int num = 0;
            for (Inventory slot : page.getPageView().slots()) {
                if (num >= 10 && num <= 16) {
                    slot.set(
                            //(spinner offset + (a buffer to prevent neg numbers + (sel slot + 1 offset) - 3 for centering) + (slotnum rel to center) % slot count
                            crate.getSlot(((spinnerOffset + (crate.getSlotCount() * 3) + (selectedSlot + 1) - 3) + (num - 10)) % crate.getSlotCount())
                                    .getDisplayItem()
                                    .toItemStack()
                    );
                }
                num++;
            }

            if (currentTicks >= currentTickDelay) {
                currentTicks = 0;
                currentTickDelay *= config.getTickDelayMultiplier();
                spinnerOffset++;
                page.getObserver().playSound(
                        (winCondition())?
                                SoundTypes.ENTITY_FIREWORK_LAUNCH:
                                SoundTypes.UI_BUTTON_CLICK, page.getObserver().getLocation().getPosition(), 0.5);
            }
            currentTicks++;
        }else{
            if(page.getTicks() % 5 == 0) {
                int num = 0;
                for (Inventory slot : page.getPageView().slots()) {
                    if (num != 13) {
                        slot.set(getConfetti());
                    }
                    num++;
                }
            }
            if(page.getTicks() > tickWinBegin + 20*3){
                crate.getSlot(selectedSlot).rewardPlayer(player,this.physicalLocation);
                page.getObserver().playSound(SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP, page.getObserver().getLocation().getPosition(), 0.5);
                rewardGiven = true;
                page.getObserver().closeInventory();
            }
        }
    }

    public static class Config extends ViewConfig {
        private Item selectorItem;
        private Integer ticksToSelection;
        private Double tickDelayMultiplier;
        private Integer ticksToSelectionVariance;
        public Config(ConfigurationNode node){
            super(node);
            if(!node.getNode("selectorItem").isVirtual()) {
                this.selectorItem = new Item(node.getNode("selectorItem"));
            }else{
                this.selectorItem = new Item("&6HuskyCrates", ItemTypes.REDSTONE_TORCH,null,1,null,null,null,null);
            }

            this.ticksToSelection = node.getNode("ticksToSelection").getInt(30);
            this.tickDelayMultiplier = node.getNode("tickDelayMultiplier").getDouble(1.08);
            this.ticksToSelectionVariance = node.getNode("ticksToSelectionVariance").getInt(0);
        }

        public Item getSelectorItem() {
            return selectorItem;
        }

        public Integer getTicksToSelection() {
            return ticksToSelection;
        }

        public Double getTickDelayMultiplier() {
            return tickDelayMultiplier;
        }

        public Integer getTicksToSelectionVariance() {
            return ticksToSelectionVariance;
        }
    }
}