package at.michael1011.backpacks.nbt;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.michael1011.backpacks.Main.serverPackage;
import static at.michael1011.backpacks.Main.version;

@SuppressWarnings({"unchecked", "PrimitiveArrayArgumentToVarargsMethod", "SameParameterValue"})
public class NbtDecoder {

    private static Class nbtBaseClass;

    public static ItemStack decodeNbt(String nbt, ItemStack item) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {

        nbtBaseClass = Class.forName(serverPackage + "NBTBase");

        Class craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");

        Object nbtCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        Object nbtTag = nbtCopy.getClass().getMethod("getTag").invoke(nbtCopy);

        if (nbtTag == null) {
            nbtTag = Class.forName(serverPackage + "NBTTagCompound").getConstructor().newInstance();
        }

        decodeCompound(nbt, nbtTag);

        nbtCopy.getClass().getMethod("setTag", nbtTag.getClass()).invoke(nbtCopy, nbtTag);

        return ((ItemStack) craftItemStack.getMethod("asBukkitCopy", nbtCopy.getClass()).invoke(null, nbtCopy));
    }

    private static void decodeCompound(String value, Object compound) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        List<String> split = decodeString(value);

        for (String part : split) {
            String[] keyValue = part.split(":", 2);

            Object nbtBase = decodeNbtBase(keyValue[1]);

            if (nbtBase != null) {
                compound.getClass().getMethod("set", String.class, nbtBaseClass)
                        .invoke(compound, keyValue[0], nbtBase);
            }
        }
    }

    private static Object decodeNbtBase(String value) throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, ClassNotFoundException, InstantiationException {

        Object toReturn = null;

        int typeLength = 1;

        if (value.startsWith("10")) {
            typeLength = 2;
        }

        int type = Integer.valueOf(value.substring(0, typeLength));
        value = value.substring(typeLength+1, value.length()-1);

        if (!value.equals("")) {
            switch (type) {
                case 1:
                    toReturn = Class.forName(serverPackage + "NBTTagByte")
                            .getConstructor(byte.class).newInstance(Byte.parseByte(value));

                    break;

                case 2:
                    toReturn = Class.forName(serverPackage + "NBTTagShort")
                            .getConstructor(short.class).newInstance(Short.parseShort(value));

                    break;

                case 3:
                    toReturn = Class.forName(serverPackage + "NBTTagInt")
                            .getConstructor(int.class).newInstance(Integer.parseInt(value));

                    break;

                case 4:
                    toReturn = Class.forName(serverPackage + "NBTTagLong")
                            .getConstructor(long.class).newInstance(Long.parseLong(value));

                    break;

                case 5:
                    toReturn = Class.forName(serverPackage + "NBTTagFloat")
                            .getConstructor(float.class).newInstance(Float.parseFloat(value));

                    break;

                case 6:
                    toReturn = Class.forName(serverPackage + "NBTTagDouble")
                            .getConstructor(double.class).newInstance(Double.parseDouble(value));

                    break;

                case 8:
                    toReturn = Class.forName(serverPackage + "NBTTagString")
                            .getConstructor(String.class).newInstance(value);

                    break;

                case 7:
                    String[] rawBytes = value.split(",");

                    byte[] bytes = new byte[]{};

                    int i = 0;

                    for (String singleByte : rawBytes) {
                        bytes[i] = Byte.parseByte(singleByte);

                        i++;
                    }

                    toReturn = Class.forName(serverPackage + "NBTTagByteArray")
                            .getConstructor(byte[].class).newInstance(bytes);

                    break;

                case 11:
                    String[] rawInts = value.split(",");

                    int[] ints = new int[]{};

                    i = 0;

                    for (String singleInt : rawInts) {
                        ints[i] = Integer.parseInt(singleInt);

                        i++;
                    }

                    toReturn = Class.forName(serverPackage + "NBTTagIntArray")
                            .getConstructor(int[].class).newInstance(ints);

                    break;

                case 9:
                    Object list = Class.forName(serverPackage + "NBTTagList").getConstructor().newInstance();

                    List<String> split = Arrays.asList(value.split("/+(?![^{]*})"));

                    for (String part : split) {
                        Object nbtBase = decodeNbtBase(part);

                        if (nbtBase != null) {
                            list.getClass().getMethod("add", nbtBaseClass)
                                    .invoke(list, nbtBase);
                        }
                    }

                    toReturn = list;

                    break;

                case 10:
                    Object compound = Class.forName(serverPackage + "NBTTagCompound").getConstructor().newInstance();

                    decodeCompound(value, compound);

                    toReturn = compound;

                    break;
            }

        }

        return toReturn;
    }

    private static List<String> decodeString(String split) {
        List<String> toReturn = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        int bracesToClose = 0;

        char[] chars = split.toCharArray();

        for (int position = 0; position < chars.length; position++) {
            char atPosition = chars[position];

            switch (chars[position]) {
                case '{':
                    buffer.append(atPosition);

                    bracesToClose++;
                    break;

                case '}':
                    buffer.append(atPosition);

                    bracesToClose--;
                    break;

                case '/':
                    if (bracesToClose == 0) {
                        toReturn.add(buffer.toString());

                        buffer.delete(0, buffer.length());

                    } else {
                        buffer.append(atPosition);
                    }

                    break;

                default:
                    buffer.append(atPosition);
                    break;
            }

        }

        toReturn.add(buffer.toString());

        return toReturn;
    }

}
