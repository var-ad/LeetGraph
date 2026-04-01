import 'package:flutter_test/flutter_test.dart';
import 'package:leetgraph/main.dart';

void main() {
  testWidgets('setup screen renders', (WidgetTester tester) async {
    await tester.pumpWidget(const LeetGraphApp());
    await tester.pump();

    expect(find.text('LeetGraph'), findsOneWidget);
    expect(find.text('Widget setup'), findsOneWidget);
    expect(find.text('Save and sync'), findsOneWidget);
  });
}
