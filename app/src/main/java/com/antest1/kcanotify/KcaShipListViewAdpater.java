package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.R.id.list;
import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaShipListViewAdpater extends BaseAdapter {
    private long exp_sum = 0L;
    private List<JsonObject> listViewItemList = new ArrayList<>();

    private static final String[] total_key_list = {
            "api_id", "api_lv", "api_stype", "api_cond", "api_locked",
            "api_karyoku", "api_raisou", "api_taiku", "api_soukou", "api_yasen",
            "api_taisen", "api_kaihi", "api_sakuteki", "api_lucky", "api_soku", "api_sally_area"};

    public long getTotalExp() { return exp_sum; }

    public static int getKeyIndex(String key) {
        for (int i = 0; i < total_key_list.length; i++) {
            if (total_key_list[i].equals(key)) return i;
        }
        return -1;
    }

    public static boolean isList(int idx) {
        int[] list = {2, 14, 15};  // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isBoolean(int idx) {
        int[] list = {4}; // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isNumeric(int idx) {
        return !isList(idx) && !isBoolean(idx);
    }

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_shiplist_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.ship_stat_2_0 = v.findViewById(R.id.ship_stat_2_0);
            holder.ship_id = v.findViewById(R.id.ship_id);
            holder.ship_name = v.findViewById(R.id.ship_name);
            holder.ship_karyoku = v.findViewById(R.id.ship_karyoku);
            holder.ship_raisou = v.findViewById(R.id.ship_raisou);
            holder.ship_taiku = v.findViewById(R.id.ship_taiku);
            holder.ship_soukou = v.findViewById(R.id.ship_soukou);
            holder.ship_stype = v.findViewById(R.id.ship_stype);
            holder.ship_lv = v.findViewById(R.id.ship_lv);
            holder.ship_hp = v.findViewById(R.id.ship_hp);
            holder.ship_cond = v.findViewById(R.id.ship_cond);
            holder.ship_exp = v.findViewById(R.id.ship_exp);
            holder.ship_kaihi = v.findViewById(R.id.ship_kaihi);
            holder.ship_sakuteki = v.findViewById(R.id.ship_sakuteki);
            holder.ship_luck = v.findViewById(R.id.ship_luck);
            holder.ship_yasen = v.findViewById(R.id.ship_yasen);
            holder.ship_taisen = v.findViewById(R.id.ship_taisen);
            holder.ship_sally_area = v.findViewById(R.id.ship_sally_area);
            holder.ship_equip_slot = new TextView[5];
            holder.ship_equip_icon = new ImageView[5];
            for (int i = 0; i < 5; i++) {
                holder.ship_equip_slot[i] = v.findViewById(getId(KcaUtils.format("ship_equip_%d_slot", i + 1), R.id.class));
            }
            for (int i = 0; i < 5; i++) {
                holder.ship_equip_icon[i] = v.findViewById(getId(KcaUtils.format("ship_equip_%d_icon", i + 1), R.id.class));
            }
            holder.ship_equip_slot_ex = v.findViewById(R.id.ship_equip_ex_slot);
            holder.ship_equip_icon_ex = v.findViewById(R.id.ship_equip_ex_icon);
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        int kc_ship_id = item.get("api_ship_id").getAsInt();
        JsonObject kcShipData = getKcShipDataById(kc_ship_id, "name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num");
        String ship_name = kcShipData.get("name").getAsString();
        int ship_stype = kcShipData.get("stype").getAsInt();
        int ship_init_ka = kcShipData.getAsJsonArray("houg").get(0).getAsInt();
        int ship_init_ra = kcShipData.getAsJsonArray("raig").get(0).getAsInt();
        int ship_init_ta = kcShipData.getAsJsonArray("tyku").get(0).getAsInt();
        int ship_init_so = kcShipData.getAsJsonArray("souk").get(0).getAsInt();
        int ship_init_lk = kcShipData.getAsJsonArray("luck").get(0).getAsInt();
        int ship_afterlv = kcShipData.get("afterlv").getAsInt();
        int ship_slot_num = kcShipData.get("slot_num").getAsInt();

        JsonArray ship_slot = item.getAsJsonArray("api_slot");
        JsonArray ship_onslot = item.getAsJsonArray("api_onslot");
        int ship_slot_ex = item.get("api_slot_ex").getAsInt();
        int ship_ex_item_icon = 0;
        int ship_locked = item.get("api_locked").getAsInt();

        int slot_sum = 0;
        boolean flag_931 = false;
        JsonArray ship_item_icon = new JsonArray();
        for (int j = 0; j < ship_slot.size(); j++) {
            int item_id = ship_slot.get(j).getAsInt();
            if (item_id > 0) {
                JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "id,type");
                if (itemData != null) {
                    int item_kc_id = itemData.get("id").getAsInt();
                    if (item_kc_id == 82 || item_kc_id == 83) flag_931 = true;
                    int item_type = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                    ship_item_icon.add(item_type);
                }
            } else {
                ship_item_icon.add(0);
            }
        }
        if (ship_slot_ex > 0) {
            JsonObject ex_item_data = getUserItemStatusById(ship_slot_ex, "level,alv", "type");
            if (ex_item_data != null) {
                ship_ex_item_icon = ex_item_data.get("type").getAsJsonArray().get(3).getAsInt();
            }
        }

        for (int j = 0; j < ship_onslot.size(); j++) {
            slot_sum += ship_onslot.get(j).getAsInt();
        }

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.ship_id.setText(item.get("api_id").getAsString());
        if (ship_locked > 0) {
            holder.ship_id.setTextColor(ContextCompat.getColor(context, R.color.colorStatLocked));
        } else {
            holder.ship_id.setTextColor(ContextCompat.getColor(context, R.color.colorStatNotLocked));
        }

        holder.ship_stype.setText(KcaApiData.getShipTypeAbbr(ship_stype));
        holder.ship_name.setText(KcaApiData.getShipTranslation(ship_name, false));

        int cond = item.get("api_cond").getAsInt();
        holder.ship_cond.setText(String.valueOf(cond));
        if (cond > 49) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipKira));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (cond / 10 >= 3) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipNormal));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (cond / 10 == 2) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipFatigue1));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipFatigue2));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        int ship_lv = item.get("api_lv").getAsInt();
        holder.ship_lv.setText(KcaUtils.format("Lv %d", ship_lv));

        if (ship_lv >= 100) {
            holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
        } else {
            holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        }

        if (ship_afterlv != 0 && ship_lv >= ship_afterlv) {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatRemodel));
            holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            if (ship_lv >= 100) {
                holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
                holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
            } else {

                holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
                holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
            }
        }

        holder.ship_exp.setText(KcaUtils.format("Next: %d", item.getAsJsonArray("api_exp").get(1).getAsInt()));

        int nowhp = item.get("api_nowhp").getAsInt();
        int maxhp = item.get("api_maxhp").getAsInt();
        if (maxhp >= 100) {
            holder.ship_hp.setText(KcaUtils.format("%d/%d", nowhp, maxhp));
        } else {
            holder.ship_hp.setText(KcaUtils.format("%d / %d", nowhp, maxhp));
        }
        if (nowhp * 4 <= maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorHeavyDmgState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else if (nowhp * 2 <= maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorModerateDmgState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else if (nowhp * 4 <= maxhp * 3) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightDmgState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (nowhp != maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNormalState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFullState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        JsonArray ship_kyouka = item.getAsJsonArray("api_kyouka");

        JsonArray ship_ka = item.getAsJsonArray("api_karyoku");
        JsonArray ship_ra = item.getAsJsonArray("api_raisou");
        JsonArray ship_ta = item.getAsJsonArray("api_taiku");
        JsonArray ship_so = item.getAsJsonArray("api_soukou");
        JsonArray ship_ts = item.getAsJsonArray("api_taisen");
        JsonArray ship_kh = item.getAsJsonArray("api_kaihi");
        JsonArray ship_st = item.getAsJsonArray("api_sakuteki");
        JsonArray ship_lk = item.getAsJsonArray("api_lucky");

        if (ship_init_ka + ship_kyouka.get(0).getAsInt() == ship_ka.get(1).getAsInt()) {
            holder.ship_karyoku.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatKaryoku));
            holder.ship_karyoku.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_karyoku.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_karyoku.setTextColor(ContextCompat.getColor(context, R.color.colorStatKaryoku));
        }
        holder.ship_karyoku.setText(ship_ka.get(0).getAsString());

        if (ship_init_ra + ship_kyouka.get(1).getAsInt() == ship_ra.get(1).getAsInt()) {
            holder.ship_raisou.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatRaisou));
            holder.ship_raisou.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_raisou.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_raisou.setTextColor(ContextCompat.getColor(context, R.color.colorStatRaisou));
        }
        holder.ship_raisou.setText(ship_ra.get(0).getAsString());

        holder.ship_yasen.setText(KcaUtils.format("%d", ship_ka.get(0).getAsInt() + ship_ra.get(0).getAsInt()));

        if (ship_init_ta + ship_kyouka.get(2).getAsInt() == ship_ta.get(1).getAsInt()) {
            holder.ship_taiku.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatTaiku));
            holder.ship_taiku.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_taiku.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_taiku.setTextColor(ContextCompat.getColor(context, R.color.colorStatTaiku));
        }
        holder.ship_taiku.setText(ship_ta.get(0).getAsString());

        if (ship_init_so + ship_kyouka.get(3).getAsInt() == ship_so.get(1).getAsInt()) {
            holder.ship_soukou.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatSoukou));
            holder.ship_soukou.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_soukou.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_soukou.setTextColor(ContextCompat.getColor(context, R.color.colorStatSoukou));
        }
        holder.ship_soukou.setText(ship_so.get(0).getAsString());

        int taisen_value = ship_ts.get(0).getAsInt();
        if (taisen_value >= 100 || (ship_stype == 1 && taisen_value >= 60) ||
                kc_ship_id == 141 || (kc_ship_id == 529 && taisen_value >= 65) ||
                ((kc_ship_id == 380 || kc_ship_id == 526) && taisen_value >= 65 && flag_931)) {
            holder.ship_taisen.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatTaisen));
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_taisen.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.colorStatTaisen));
        }
        holder.ship_taisen.setText(ship_ts.get(0).getAsString());

        holder.ship_kaihi.setText(ship_kh.get(0).getAsString());
        holder.ship_sakuteki.setText(ship_st.get(0).getAsString());

        if (ship_init_lk + ship_kyouka.get(4).getAsInt() == ship_lk.get(1).getAsInt()) {
            holder.ship_luck.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatLuck));
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_luck.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.colorStatLuck));
        }
        holder.ship_luck.setText(ship_lk.get(0).getAsString());

        for (int i = 0; i < ship_item_icon.size(); i++) {
            if (i >= holder.ship_equip_icon.length) break;
            int item_id = ship_item_icon.get(i).getAsInt();
            if (item_id == 0) {
                holder.ship_equip_icon[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else {
                holder.ship_equip_icon[i].setImageResource(
                        getId(KcaUtils.format("item_%d", item_id), R.mipmap.class));
                holder.ship_equip_icon[i].setVisibility(View.VISIBLE);
            }
        }

        if (ship_slot_ex == 0) {
            holder.ship_equip_icon_ex.setVisibility(View.INVISIBLE);
        } else {
            holder.ship_equip_icon_ex.setImageResource(
                    getId(KcaUtils.format("item_%d", ship_ex_item_icon), R.mipmap.class));
            holder.ship_equip_icon_ex.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < ship_onslot.size(); i++) {
            if (i >= holder.ship_equip_slot.length || i >= ship_slot.size()) break;
            if (slot_sum == 0) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else if (i >= ship_slot_num) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else {
                holder.ship_equip_slot[i].setText(ship_onslot.get(i).getAsString());
                holder.ship_equip_slot[i].setVisibility(View.VISIBLE);
            }
        }

        if (item.has("api_sally_area")) {
            holder.ship_sally_area.setBackgroundColor(ContextCompat.getColor(context,
                    getId(KcaUtils.format("colorStatSallyArea%d", item.get("api_sally_area").getAsInt()), R.color.class)));
            holder.ship_sally_area.setVisibility(View.VISIBLE);
        } else {
            holder.ship_sally_area.setVisibility(View.GONE);
        }
        return v;
    }

    static class ViewHolder {
        LinearLayout ship_stat_2_0;
        TextView ship_id, ship_stype, ship_name, ship_lv, ship_exp, ship_hp, ship_cond;
        TextView ship_karyoku, ship_raisou, ship_taiku, ship_soukou;
        TextView ship_yasen, ship_taisen, ship_kaihi, ship_sakuteki, ship_luck;
        TextView[] ship_equip_slot;
        ImageView[] ship_equip_icon;
        TextView ship_equip_slot_ex;
        ImageView ship_equip_icon_ex;
        ImageView ship_sally_area;
    }

    public void setListViewItemList(JsonArray ship_list, String sort_key) {
        setListViewItemList(ship_list, sort_key, "|");
    }

    public void setListViewItemList(JsonArray ship_list, String sort_key, final String filter) {
        exp_sum = 0;
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        if (listViewItemList == null) listViewItemList = new ArrayList<>();

        if (!filter.equals("|") && listViewItemList.size() > 1) {
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, new Predicate<JsonObject>() {
                @Override
                public boolean apply(JsonObject input) {
                    String[] filter_list = filter.split("\\|");
                    for (String key_op_val: filter_list) {
                        if (key_op_val.length() != 0) {
                            String[] kov_split = key_op_val.split(",");
                            int idx = Integer.valueOf(kov_split[0]);
                            String key = total_key_list[idx];
                            int op = Integer.valueOf(kov_split[1]);

                            String v1 = KcaShipListViewAdpater.getstrvalue(input, key);
                            String v2 = kov_split[2].trim();
                            String[] v2_list = v2.split("_");

                            int v1_int = KcaShipListViewAdpater.getintvalue(input, key);

                            boolean flag = false;
                            switch (op) {
                                case 0:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int == v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                case 1:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int != v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                case 2:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int < v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                case 3:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int > v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                case 4:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int <= v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                case 5:
                                    for (String v2_val: v2_list) {
                                        int v2_int = Integer.valueOf(v2_val);
                                        if (v1_int >= v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (!flag) return false;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    return true;
                }
            }));
        }

        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
        for (int i = 0; i < listViewItemList.size(); i++) {
            exp_sum += listViewItemList.get(i).getAsJsonArray("api_exp").get(0).getAsLong();
        }
    }

    private static int getintvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonArray()) {
                return o.getAsJsonArray(key).get(0).getAsInt();
            } else {
                return o.get(key).getAsInt();
            }
        } else if (key.equals("api_yasen")) {
            return getintvalue(o, "api_karyoku") + getintvalue(o, "api_raisou");
        } else {
            int kc_ship_id = o.get("api_ship_id").getAsInt();
            JsonObject kcShipData = getKcShipDataById(kc_ship_id, "api_name,api_stype");
            if (kcShipData != null && kcShipData.has(key)) {
                return kcShipData.get(key).getAsInt();
            }
        }
        return 0;
    }

    private static String getstrvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonPrimitive()) return o.get(key).getAsString();
            else return o.get(key).toString();
        }
        else return "";
    }

    public void resortListViewItem(String sort_key) {
        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
    }

    private class StatComparator implements Comparator<JsonObject> {
        String sort_key;
        private StatComparator(String key) {
            sort_key = key;
        }

        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String[] sort_key_list = sort_key.split("\\|");
            for (String key_idx: sort_key_list) {
                if (key_idx.length() != 0) {
                    int idx = Integer.valueOf(key_idx.split(",")[0]);
                    boolean is_desc = Boolean.valueOf(key_idx.split(",")[1]);
                    String key = total_key_list[idx];
                    if (key.equals("api_lv")) key = "api_exp";
                    int val1 = KcaShipListViewAdpater.getintvalue(o1, key);
                    int val2 = KcaShipListViewAdpater.getintvalue(o2, key);
                    if (val1 != val2) {
                        if (is_desc) return val2 - val1;
                        else return val1 - val2;
                    }
                }
            }
            return 0;
        }
    }
}
