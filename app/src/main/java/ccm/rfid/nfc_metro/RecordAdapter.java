package ccm.rfid.nfc_metro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ccm.rfid.nfc_metro.Models.Record;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private List<Record> records;

    public void setRecords(List<Record> records){
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_record, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecordAdapter.ViewHolder holder, int position) {
         holder.bind(records.get(position));
    }

    @Override
    public int getItemCount() {
        return (records == null) ? 0 : records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView actionTextView;
        private TextView amountTextView;
        private TextView timeTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            actionTextView = view.findViewById(R.id.action_field_textView);
            amountTextView = view.findViewById(R.id.amount_field_viewText);
            timeTextView = view.findViewById(R.id.timestamp_field_viewText);
        }

        public void bind(Record record)
        {
            actionTextView.setText(record.getAction());
            amountTextView.setText(Integer.toString(record.getAmount()));
            timeTextView.setText(record.getTimeStamp().toDate().toString());
        }
    }
}
