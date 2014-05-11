package oly.netpowerctrl.preferences;

// --Commented out by Inspection START (11.05.14 21:17):
///**
// * Integers in Preferences
// */
//class IntEditTextPreference extends EditTextPreference {
//// --Commented out by Inspection START (11.05.14 21:17):
////    public IntEditTextPreference(Context context) {
////        super(context);
////    }
//// --Commented out by Inspection STOP (11.05.14 21:17)
//
//// --Commented out by Inspection START (11.05.14 21:17):
////    public IntEditTextPreference(Context context, AttributeSet attrs) {
////        super(context, attrs);
////    }
//// --Commented out by Inspection STOP (11.05.14 21:17)
//
//// --Commented out by Inspection START (11.05.14 21:17):
////    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
////        super(context, attrs, defStyle);
////    }
//// --Commented out by Inspection STOP (11.05.14 21:17)
//
//    // --Commented out by Inspection (11.05.14 21:17):private Integer initialValue;
//
//    @Override
//    protected void onSetInitialValue(boolean restorePersistedValue,
//                                     Object defaultValue) {
//        int def = (defaultValue instanceof Number) ? (Integer) defaultValue
//                : (defaultValue != null) ? Integer.parseInt(defaultValue.toString()) : 1;
//        if (restorePersistedValue) {
//            this.initialValue = getPersistedInt(def);
//        } else this.initialValue = (Integer) defaultValue;
//    }
//
//    @Override
//    protected Object onGetDefaultValue(TypedArray a, int index) {
//        return a.getInt(index, 1);
//    }
//
//    @Override
//    protected String getPersistedString(String defaultReturnValue) {
//        try {
//            return String.valueOf(getPersistedInt(-1));
//
//        } catch (ClassCastException ignored) {
//            return super.getPersistedString(defaultReturnValue);
//        }
//    }
//
//    @Override
//    protected boolean persistString(String value) {
//        try {
//            return persistInt(Integer.valueOf(value));
//        } catch (ClassCastException ignored) {
//            return super.persistString(value);
//        }
//    }
//}
// --Commented out by Inspection STOP (11.05.14 21:17)